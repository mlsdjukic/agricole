package com.example.alarms.components;

import com.example.alarms.actions.Action;
import com.example.alarms.dto.ActionRequest;
import com.example.alarms.dto.Jobs;
import com.example.alarms.dto.RuleMapper;
import com.example.alarms.entities.ActionEntity;
import com.example.alarms.entities.ReservationEntity;
import com.example.alarms.entities.ReactionEntity;
import com.example.alarms.entities.RuleEntity;
import com.example.alarms.entities.security.SecurityAccount;
import com.example.alarms.exceptions.*;
import com.example.alarms.reactions.Reaction;
import com.example.alarms.reactions.WriteAlarmToDBReaction;
import com.example.alarms.rules.Rule;
import com.example.alarms.services.ActionService;
import com.example.alarms.services.ReservationService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.env.Environment;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Slf4j
@Component
public class Coordinator {

    private  final RuleMapper ruleMapper;
    private final ReservationService reservationService;
    private final ActionService actionService;

    private final ConcurrentHashMap<Long, JobDescription> subscriptions;

    private final Duration jobTimeout;
    private final Integer batchSize;
    private final String instanceId;
    private volatile boolean isRunning;


    public Coordinator(ActionService actionService, RuleMapper ruleMapper, ReservationService reservationService, Environment env) {
        this.actionService = actionService;
        this.ruleMapper = ruleMapper;
        this.reservationService = reservationService;

        this.subscriptions = new ConcurrentHashMap<>();

        this.jobTimeout = Duration.ofSeconds(Integer.parseInt(env.getProperty("JOB_TIMEOUT", "60")));
        this.batchSize = Integer.parseInt(env.getProperty("BATCH_SIZE", "50"));

        this.instanceId = UUID.randomUUID().toString();
        this.isRunning = false;
    }

    private final AtomicBoolean processingBatch = new AtomicBoolean(false);

    public void startLoop() {
        if (isRunning) {
            scheduleNextRun(0); // Start immediately
        }
    }

    private void scheduleNextRun(long delaySeconds) {
        if (!isRunning) {
            log.info("Job processor shutting down, not scheduling next run");
            return;
        }

        // Schedule the next run after the specified delay
        Mono.delay(Duration.ofSeconds(delaySeconds))
                .publishOn(Schedulers.boundedElastic())
                .flatMap(__ -> {
                    if (!isRunning) {
                        return Mono.empty();
                    }

                    // Add guard to prevent any possibility of concurrent execution
                    if (!processingBatch.compareAndSet(false, true)) {
                        log.warn("Attempted to start a new batch while previous batch is still running");
                        return Mono.empty();
                    }

                    log.debug("Starting job initialization batch");
                    return initializeJobs(this.batchSize - this.subscriptions.size())
                            .doOnNext(job -> log.debug("Processed job: {}", job.getId()))
                            .doOnError(error -> log.error("Error processing jobs: {}", error.getMessage(), error))
                            .onErrorResume(e -> Flux.empty())
                            .collectList()
                            .doFinally(signalType -> {
                                // Reset the processing flag
                                processingBatch.set(false);
                                // Schedule next run
                                log.debug("Job initialization batch complete with signal: {}", signalType);
                                if (isRunning) {
                                    scheduleNextRun(10);
                                }
                            });
                })
                .subscribe(
                        result -> log.debug("Completed batch with {} jobs", result.size()),
                        error -> {
                            log.error("Critical error in job scheduler: {}", error.getMessage(), error);
                            // Reset processing flag in case of error
                            processingBatch.set(false);
                            if (isRunning) {
                                scheduleNextRun(5);
                            }
                        },
                        () -> {
                            if (isRunning) {
                                log.debug("Normal completion of scheduling task");
                            }
                        }
                );
    }

    public void start() {
        isRunning = true;

        initializeJobs(this.batchSize - this.subscriptions.size())
                // Handle errors at the individual job level
                .onErrorContinue((error, ___) ->
                        log.error("Error processing job: {}", error.getMessage(), error)
                )
                .doFinally(signalType -> {
                    log.info("initial startup done");
                    startLoop();
                })
                .subscribe();




        log.info("Job processor started with instance ID: {}", instanceId);
    }

    public Flux<ReservationEntity> initializeJobs(int numOfJobs) {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minus(jobTimeout);

        // Fetch jobs from the database
        return reservationService.findAvailableJobs(timeoutThreshold, numOfJobs)
                .flatMap(job -> tryAcquireAndProcessJob(job, timeoutThreshold));
    }

    private Mono<ReservationEntity> tryAcquireAndProcessJob(ReservationEntity job, LocalDateTime timeoutThreshold) {
        return reservationService.tryAcquireJob(
                        job.getId(),
                        instanceId,
                        LocalDateTime.now(),
                        timeoutThreshold
                )
                .filter(acquired -> acquired)
                .flatMap(__ -> {
                    // Get the associated Action for this Job
                    Long actionId = job.getActionId();
                    if (actionId == null) {
                        return reservationService.releaseJob(job.getId(), instanceId)
                                .then(Mono.empty());
                    }

                    return actionService.getActionById(actionId)
                            .flatMap(action ->
                                    // Process the job reactively and then return the job
                                    scheduleJob(action, job.getId())
                                            .thenReturn(job)
                            )
                            .switchIfEmpty(Mono.defer(() ->
                                    // Return a Mono that completes only after releasing the job
                                    reservationService.releaseJob(job.getId(), instanceId)
                                            .then(Mono.empty())
                            ));
                });
    }

    public Action createAction(ActionEntity actionEntity) throws Exception {
        String actionClassName = "com.example.alarms.actions." + actionEntity.getType() + "." + actionEntity.getType();
        return (Action) createInstance(actionClassName, new Class<?>[]{String.class, Long.class}, actionEntity.getParams(), actionEntity.getId());
    }

    public List<Rule> createRules(List<RuleEntity> ruleEntities) {
        return ruleEntities.stream()
                .map(this::createRule)
                .collect(Collectors.toList());
    }

    private Rule createRule(RuleEntity ruleEntity) {
        List<Reaction> reactions = Stream.concat(
                ruleEntity.getReactions().stream().map(this::createReaction), // Existing reactions
                Stream.of(new WriteAlarmToDBReaction(ruleEntity.getId()) ) // adding write alarm to db as default reaction
        ).toList();

        String ruleClassName = "com.example.alarms.rules." + ruleEntity.getName() + "." + ruleEntity.getName();
        try {
            return (Rule) createInstance(ruleClassName,
                    new Class<?>[]{String.class, Long.class, List.class},
                    ruleEntity.getDefinition(), ruleEntity.getId(), reactions);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Rule: " + ruleEntity.getName(), e);
        }
    }

    private Reaction createReaction(ReactionEntity reactionEntity) {
        String reactionClassName = "com.example.alarms.reactions." + reactionEntity.getName() + "." + reactionEntity.getName();
        try {
            return (Reaction) createInstance(reactionClassName,
                    new Class<?>[]{String.class, String.class, Long.class},
                    reactionEntity.getParams(), reactionEntity.getName(), reactionEntity.getRuleId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Reaction: " + reactionEntity.getName(), e);
        }
    }


    private Object createInstance(String className, Class<?>[] parameterTypes, Object... args) throws Exception {
        Class<?> clazz = Class.forName(className);
        Constructor<?> constructor = clazz.getDeclaredConstructor(parameterTypes);
        return constructor.newInstance(args);
    }

    /**
     * Schedules a job based on the provided action entity.
     * @return Mono<Void> that completes when the job is scheduled
     */
    private Mono<Void> scheduleJob(ActionEntity actionEntity, Long jobId) {
        log.info("Scheduling job: {} with action: {}", jobId, actionEntity.getType());

        // Create action and rules using reactive error handling
        return createActionAndRules(actionEntity)
                .flatMap(components -> {
                    Action action = components.getT1();
                    List<Rule> rules = components.getT2();

                    // First execution immediately
                    Flux<Void> initialExecution = executeActionAndRules(action, rules)
                            .onErrorResume(e -> {
                                log.error("Error in initial execution: {}", e.getMessage(), e);
                                return Flux.empty();
                            });

                    // Setup periodic execution
                    return initialExecution
                            .then() // Convert Flux<Void> to Mono<Void>
                            .doOnSuccess(__ -> setupPeriodicExecution(action, rules, actionEntity, jobId));
                })
                .onErrorResume(e -> {
                    log.error("Failed to schedule job {}: {}", jobId, e.getMessage());
                    return Mono.empty();
                })
                ;
    }

    /**
     * Creates the action and rules in a reactive way.
     */
    private Mono<Tuple2<Action, List<Rule>>> createActionAndRules(ActionEntity actionEntity) {
        return Mono.defer(() -> {
                    log.debug("Mono.defer was subscribed to");
                    try {
                        log.debug("Entering try block");
                        Action action = createAction(actionEntity);
                        List<Rule> rules = createRules(actionEntity.getRules());
                        return Mono.just(Tuples.of(action, rules));
                    } catch (Exception e) {
                        Throwable original = e.getCause();
                        String errorMessage = original != null ? original.getMessage() : e.getMessage();
                        log.error("Failed to create action/rules: {}", errorMessage);
                        return Mono.error(e);
                    }
                })
                .doOnSubscribe(sub -> log.debug("Mono was subscribed to"));
    }

    /**
     * Executes an action and its associated rules.
     */
    private Flux<Void> executeActionAndRules(Action action, List<Rule> rules) {
        return action.execute()
                .flatMap(data -> Flux.fromIterable(rules)
                        .doOnNext(rule -> rule.execute(data))
                        .then());
    }

    /**
     * Sets up periodic execution of an action and its rules.
     */
    private void setupPeriodicExecution(Action action, List<Rule> rules,
                                        ActionEntity actionEntity, Long jobId) {

        AtomicInteger counter = new AtomicInteger(0);
        int maxRetries = 5;
        Duration retryBackoff = Duration.ofSeconds(2);
        Duration heartbeatTimeout = Duration.ofSeconds(5);

        Disposable subscription = Flux.interval(Duration.ofSeconds(action.getInterval()))
                .publishOn(Schedulers.boundedElastic())
                .concatMap(tick -> {
                    Flux<Void> actionFlux = executeActionAndRules(action, rules);

                    // Update heartbeat only every 6 ticks (60 seconds if interval is 10 seconds)
                    if (counter.incrementAndGet() % 6 == 0) {
                        return actionFlux.thenMany(
                                reservationService.updateHeartbeat(jobId, instanceId, LocalDateTime.now())
                                        .onErrorResume(err -> {
                                            log.error("Failed to update heartbeat for job {}: {}", jobId, err.getMessage());
                                            // Continue despite heartbeat errors
                                            return Mono.empty();
                                        })
                                        .thenMany(Flux.empty())
                        );
                    }
                    return actionFlux;

                })
                .onErrorResume(throwable -> {
                    log.error("Error in action execution of a job {} with message: {}", jobId, throwable.getMessage());
                    return Flux.empty();
                })
                .subscribe();

        // Store subscription information
        Jobs jobs = new Jobs(
                actionEntity.getId(),
                actionEntity.getType(),
                action.getExposedParamsJson(),
                actionEntity.getRules().stream().map(ruleMapper::toDTO).toList()
        );

        subscriptions.put(actionEntity.getId(), new JobDescription(subscription, jobId, jobs));
    }

    public Mono<Void> stopAction(Long actionId) {
        if (actionId == null) {
            return Mono.error(new IllegalArgumentException("Action ID cannot be null"));
        }

        // Retrieve the job description
        JobDescription jobDescription = subscriptions.get(actionId);

        // If no job found, return an appropriate error
        if (jobDescription == null) {
            log.warn("No active job found for action ID: {}", actionId);
            return Mono.empty();
        }

        // Dispose the subscription if it exists
        Disposable subscription = jobDescription.getDisposable();
        if (subscription != null && !subscription.isDisposed()) {
            try {
                subscription.dispose();
                log.info("Successfully disposed subscription for action ID: {}", actionId);
            } catch (Exception e) {
                log.error("Error disposing subscription for action ID {}: {}", actionId, e.getMessage(), e);
                // Continue execution despite disposal error
            }
        } else {
            log.warn("Subscription for action ID {} was already disposed or null", actionId);
        }

        // Remove from subscriptions map
        JobDescription removed = subscriptions.remove(actionId);
        if (removed != null) {
            log.debug("Removed job description from subscriptions map for action ID: {}", actionId);
        }

        // Release the job and return the reactive result
        return reservationService.releaseJob(jobDescription.getJobId(), this.instanceId)
                .doOnSuccess(v -> log.info("Successfully released job {} for action ID {}",
                        jobDescription.getJobId(), actionId))
                .doOnError(error -> log.error("Error releasing job {} for action ID {}: {}",
                        jobDescription.getJobId(), actionId, error.getMessage(), error))
                .onErrorResume(error -> {
                    // Note: We're not propagating errors from releaseJob since we've already
                    // disposed of the subscription and removed it from our tracking map.
                    // The job stopping is considered successful even if the release fails.
                    log.warn("Error occurred while releasing job {} for action ID {}, but action was stopped",
                            jobDescription.getJobId(), actionId);
                    return Mono.empty();
                });
    }

    public boolean isActionRunning(Long actionId) {
        // Check if the actionId has an active subscription
        return subscriptions.containsKey(actionId);
    }

    public Mono<ActionEntity> create(ActionRequest action, Object authentication) {
        // Extract user ID from authentication context
        Long userId = authentication instanceof SecurityAccount sc ? sc.getAccount().getId() : null;

        if (userId == null) {
            return Mono.error(new UserNotFoundException("User ID is null"));
        }

        return actionService.create(action, userId)
                .flatMap(actionEntity -> {
                    // Create a job and properly chain it in the reactive flow
                    ReservationEntity job = new ReservationEntity(
                            null,               // id
                            "pending",               // description
                            this.instanceId,             // status or identifier
                            actionEntity.getId(), // actionId
                            null,               // lockedBy
                            LocalDateTime.now() // createdAt
                    );

                    return reservationService.save(job)
                            .thenReturn(actionEntity);
                })
                .onErrorMap(ex -> {
                    // Don't wrap our custom exceptions
                    if (ex instanceof UserNotFoundException ||
                            ex instanceof InvalidActionException ||
                            ex instanceof RuleProcessingException ||
                            ex instanceof SerializationException) {
                        return ex;
                    }
                    // More detailed error logging
                    log.error("Failed to create action: {}", ex.getMessage(), ex);
                    return new RuntimeException("Failed to create action", ex);
                });
    }

    public Mono<ActionEntity> update(ActionRequest action, Long id) {
            return this.stopAction(id)
                .then(actionService.update(action, id))
                .onErrorMap(ex -> {
                    // Don't wrap our custom exceptions
                    if (ex instanceof UserNotFoundException ||
                            ex instanceof InvalidActionException ||
                            ex instanceof RuleProcessingException ||
                            ex instanceof EntityNotFoundException ||
                            ex instanceof IllegalArgumentException ||
                            ex instanceof SerializationException) {
                        return ex;
                    }
                    // More detailed error logging
                    log.error("Failed to update action: {}", ex.getMessage(), ex);
                    return new RuntimeException("Failed to update action", ex);
                });    }


    public Mono<Void> delete(Long actionId) {
        return this.stopAction(actionId)  // Now returns Mono<Void> directly
                .then(actionService.deleteAction(actionId))
                .then(reservationService.deleteByActionId(actionId))
                .doOnSuccess(__ -> subscriptions.remove(actionId))
                .onErrorResume(ex -> {
                    log.error("Failed to delete action {}: {}", actionId, ex.getMessage(), ex);
                    return Mono.error(new RuntimeException("Failed to delete action", ex));
                });
    }

//    public Flux<JobsDTO> getJobs() {
//        return Flux.fromIterable(subscriptions.values())
//                .map(JobDescription::getJob);
//    }

    public Flux<ActionEntity> get(Pageable pageable) {
        return actionService.getAll(pageable)
                .onErrorMap(e -> {
                    log.error("Error in coordinator while fetching actions: {}", e.getMessage());
                    return e instanceof IllegalArgumentException ?
                            e : new RuntimeException("Failed to retrieve actions", e);
                });
    }

    public Mono<ActionEntity> get(Long id) {
        return actionService.getActionById(id)
                .onErrorMap(e -> {
                    log.error("Error in coordinator while fetching action {} with id: {}", e.getMessage(), id);
                    return e instanceof IllegalArgumentException ?
                            e : new RuntimeException("Failed to retrieve actions", e);
                });
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class JobDescription {
        private Disposable disposable;
        private Long jobId;
        private Jobs job;
    }
}
