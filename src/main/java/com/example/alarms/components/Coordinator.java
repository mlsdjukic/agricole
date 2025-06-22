package com.example.alarms.components;

import com.example.alarms.actions.IAction;
import com.example.alarms.dto.Action;
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
import java.util.Optional;
import java.util.Set;
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

    private final ReservationService reservationService;
    private final ActionService actionService;

    private  final RuleMapper ruleMapper;

    private final ConcurrentHashMap<Long, JobDescription> subscriptions;

    private final Duration jobTimeout;
    private final Integer batchSize;
    private final String instanceId;
    private volatile boolean isRunning;


    public Coordinator(ActionService actionService, ReservationService reservationService, RuleMapper ruleMapper, Environment env) {
        this.actionService = actionService;
        this.reservationService = reservationService;
        this.ruleMapper = ruleMapper;

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

        setupHeartbeat();
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

    private Disposable setupHeartbeat() {
        return Flux.interval(Duration.ofSeconds(30))
                .publishOn(Schedulers.boundedElastic())
                .flatMap(tick -> Flux.fromIterable(subscriptions.entrySet())
                        .flatMap(entry -> {
                            Long jobId = entry.getValue().getJobId(); // assuming this method exists
                            return Mono.fromRunnable(() ->
                                            reservationService.updateHeartbeat(jobId, instanceId, LocalDateTime.now())
                                    )
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .onErrorResume(err -> {
                                        log.error("Failed to update heartbeat for job {}: {}", jobId, err.getMessage());
                                        return Mono.empty();
                                    });
                        }))
                .subscribe();
    }


    public Flux<ReservationEntity> initializeJobs(int numOfJobs) {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minus(jobTimeout);

        // Fetch jobs from the database
        return Flux.fromIterable(reservationService.findAvailableJobs(timeoutThreshold, numOfJobs))
                .flatMap(job -> tryAcquireAndProcessJob(job, timeoutThreshold));
    }

    private Mono<ReservationEntity> tryAcquireAndProcessJob(ReservationEntity job, LocalDateTime timeoutThreshold) {

        return Mono.fromCallable(() ->
                        reservationService.tryAcquireJob(
                                job.getId(),
                                instanceId,
                                LocalDateTime.now(),
                                timeoutThreshold
                        )
                )
                .subscribeOn(Schedulers.boundedElastic())
                .filter(acquired -> acquired > 0)
                .flatMap(__ -> {
                    Long actionId = job.getAction() != null ? job.getAction().getId() : null;

                    if (actionId == null) {
                        return Mono.fromRunnable(() ->
                                        reservationService.releaseJob(job.getId(), instanceId)
                                ).subscribeOn(Schedulers.boundedElastic())
                                .then(Mono.empty());
                    }

                    return Mono.fromCallable(() -> actionService.getActionByIdWithRulesAndReactions(actionId))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(action -> {
                                return action.map(actionEntity -> scheduleJob(actionEntity, job.getId())
                                        .thenReturn(job)).orElseGet(() -> Mono.fromRunnable(() ->
                                                reservationService.releaseJob(job.getId(), instanceId)
                                        ).subscribeOn(Schedulers.boundedElastic())
                                        .then(Mono.empty()));

                            });
                });
    }


    public IAction createAction(ActionEntity actionEntity) throws Exception {
        String actionClassName = "com.example.alarms.actions." + actionEntity.getType() + "." + actionEntity.getType();
        return (IAction) createInstance(actionClassName, new Class<?>[]{String.class, Long.class}, actionEntity.getParams(), actionEntity.getId());
    }

    public Set<Rule> createRules(Set<RuleEntity> ruleEntities) {
        return ruleEntities.stream()
                .map(this::createRule)
                .collect(Collectors.toSet());
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
            Long ruleId = reactionEntity.getRule() != null ? reactionEntity.getRule().getId() : null;

            return (Reaction) createInstance(
                    reactionClassName,
                    new Class<?>[]{String.class, String.class, Long.class},
                    reactionEntity.getParams(),
                    reactionEntity.getName(),
                    ruleId);
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
                    IAction action = components.getT1();
                    Set<Rule> rules = components.getT2();

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
    private Mono<Tuple2<IAction, Set<Rule>>> createActionAndRules(ActionEntity actionEntity) {
        return Mono.defer(() -> {
                    log.debug("Mono.defer was subscribed to");
                    try {
                        log.debug("Entering try block");
                        IAction action = createAction(actionEntity);
                        Set<Rule> rules = createRules(actionEntity.getRules());
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
    private Flux<Void> executeActionAndRules(IAction action, Set<Rule> rules) {
        return action.execute()
                .flatMap(data -> Flux.fromIterable(rules)
                        .doOnNext(rule -> rule.execute(data))
                        .then())
                .onErrorResume(e -> {
                    log.error("Error: {}", e.getMessage(), e);
                    return Flux.empty();
                });
    }

    /**
     * Sets up periodic execution of an action and its rules.
     */
    private void setupPeriodicExecution(IAction action, Set<Rule> rules,
                                        ActionEntity actionEntity, Long jobId) {

        Disposable subscription = Flux.interval(Duration.ofSeconds(action.getInterval()))
                .publishOn(Schedulers.boundedElastic())
                .concatMap(tick -> executeActionAndRules(action, rules)
                        .onErrorResume(err -> {
                            log.error("Error executing action: {}", err.getMessage());
                            return Flux.empty(); // Continue on error
                        }))
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

    public void  stopAction(Long actionId) {
        if (actionId == null) {
            throw new IllegalArgumentException("Action ID cannot be null");
        }

        // Retrieve the job description
        JobDescription jobDescription = subscriptions.get(actionId);

        // If no job found, return an appropriate error
        if (jobDescription == null) {

            log.warn("No active job found for action ID: {}", actionId);
            return;
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

        try {
            reservationService.releaseJob(jobDescription.getJobId(), this.instanceId);
            log.info("Successfully released job {} for action ID {}", jobDescription.getJobId(), actionId);
        } catch (Exception error) {
            log.warn("Error occurred while releasing job {} for action ID {}, but action was stopped",
                    jobDescription.getJobId(), actionId);
        }
    }

    public boolean isActionRunning(Long actionId) {
        // Check if the actionId has an active subscription
        return subscriptions.containsKey(actionId);
    }

    public ActionEntity create(Action action, Object authentication) {
        // Extract user ID from authentication context
        Long userId = authentication instanceof SecurityAccount sc ? sc.getAccount().getId() : null;

        if (userId == null) {
            throw new UserNotFoundException("User ID is null");
        }
        try {
            // Create the action
            ActionEntity actionEntity = actionService.create(action, userId); // must be blocking

            // Create a job and properly chain it in the reactive flow
            ReservationEntity job = new ReservationEntity(
                    null,               // id
                    "pending",               // description
                    this.instanceId,             // status or identifier
                    null,               // createdDate
                    LocalDateTime.now(), // createdAt
                    actionEntity // actionId
            );

            reservationService.save(job); // must be blocking

            return actionEntity;

        } catch (InvalidActionException |
                 RuleProcessingException |
                 SerializationException e) {
            throw e; // rethrow known exceptions
        } catch (Exception ex) {
            log.error("Failed to create action: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to create action", ex);
        }
    }

    public ActionEntity update(Action action, Long id) {
        try {
            // First, stop the action
            this.stopAction(id); // this must now be imperative

            // Then update the action
            return actionService.update(action, id); // imperative method

        } catch (UserNotFoundException |
                 InvalidActionException |
                 RuleProcessingException |
                 EntityNotFoundException |
                 IllegalArgumentException |
                 SerializationException ex) {
            throw ex; // preserve original exception
        } catch (Exception ex) {
            log.error("Failed to update action: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to update action", ex);
        }
    }


    public void delete(Long actionId) {
            // Stop the action (already imperative now)
            this.stopAction(actionId);

            Optional<ActionEntity> actionEntity = actionService.getActionById(actionId);
            if (actionEntity.isEmpty()){
                throw new EntityNotFoundException("Action with id " +  actionId + " not found");
            }
            // Delete related reservation
            reservationService.deleteByActionId(actionEntity.get()); // must be imperative
            // Delete the action
            actionService.deleteAction(actionId); // must be imperative


            // Clean up subscriptions
            subscriptions.remove(actionId);

    }


    public List<ActionEntity> get(Pageable pageable) {
        return actionService.getAll(pageable); // assuming a standard JPA call
    }

    public Optional<ActionEntity> get(Long id) {
        return actionService.getActionById(id);
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
