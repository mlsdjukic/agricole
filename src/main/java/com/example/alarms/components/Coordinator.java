package com.example.alarms.components;

import com.example.alarms.actions.Action;
import com.example.alarms.dto.ActionDTO;
import com.example.alarms.dto.JobsDTO;
import com.example.alarms.dto.RuleMapper;
import com.example.alarms.entities.ActionEntity;
import com.example.alarms.entities.ReservationEntity;
import com.example.alarms.entities.ReactionEntity;
import com.example.alarms.entities.RuleEntity;
import com.example.alarms.entities.security.SecurityAccount;
import com.example.alarms.reactions.Reaction;
import com.example.alarms.reactions.WriteAlarmToDBReaction;
import com.example.alarms.rules.Rule;
import com.example.alarms.services.ActionService;
import com.example.alarms.services.ReservationService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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

        this.jobTimeout = Duration.ofSeconds(Integer.parseInt(env.getProperty("JOB_TIMEOUT", "300")));
        this.batchSize = Integer.parseInt(env.getProperty("BATCH_SIZE", "50"));

        this.instanceId = UUID.randomUUID().toString();
        this.isRunning = false;
    }

    public void startLoop() {
        // Main job acquisition loop with error handling
        Flux.interval(Duration.ofSeconds(10))
                .publishOn(Schedulers.boundedElastic())
                .takeWhile(__ -> isRunning)
                .flatMap(__ ->


                        initializeJobs(this.batchSize - this.subscriptions.size())
                                // Handle errors at the individual job level
                                .onErrorContinue((error, ___) ->
                                        log.error("Error processing job: {}", error.getMessage(), error)
                                )
                )
                // Handle errors at the entire stream level to prevent termination
                .onErrorResume(error -> {
                    log.error("Critical error in job acquisition loop: {}", error.getMessage(), error);
                    // Sleep a bit to avoid aggressive retries on persistent errors
                    return Mono.delay(Duration.ofSeconds(5)).thenMany(Flux.empty());
                })
                // Restart the stream if it completes unexpectedly
                .repeat(() -> isRunning)
                .subscribe(
                        job -> {}, // We're handling each job inside acquireJobs
                        error -> {
                            // This should never be reached due to onErrorResume, but just in case
                            log.error("Fatal error in job processor: {}", error.getMessage(), error);
                            // Attempt to restart the job processor
                            if (isRunning) {
                                log.info("Attempting to restart job processor after fatal error");
                                start();
                            }
                        },
                        () -> {
                            // This executes when the stream completes normally (e.g., during shutdown)
                            log.info("Job processor shut down cleanly");
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
                            .flatMap(action -> {
                                // Start processing in a separate thread
                                scheduleJob(action, job.getId()).subscribe();
                                return Mono.just(job);
                            })
                            .switchIfEmpty(Mono.defer(() -> {

                                // Return a Mono that completes only after releasing the job
                                return reservationService.releaseJob(job.getId(), instanceId)
                                        .then(Mono.empty());
                            }));
                });
    }

    public Action createAction(ActionEntity actionEntity) throws Exception {
        String actionClassName = "com.example.alarms.actions." + actionEntity.getType();
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

        String ruleClassName = "com.example.alarms.rules." + ruleEntity.getName();
        try {
            return (Rule) createInstance(ruleClassName,
                    new Class<?>[]{String.class, Long.class, List.class},
                    ruleEntity.getRule(), ruleEntity.getId(), reactions);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Rule: " + ruleEntity.getName(), e);
        }
    }

    private Reaction createReaction(ReactionEntity reactionEntity) {
        String reactionClassName = "com.example.alarms.reactions." + reactionEntity.getName();
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
                    log.error("Failed to schedule job {}: {}", jobId, e.getMessage(), e);
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
        Disposable subscription = Flux.interval(Duration.ofSeconds(action.getInterval()))
                .publishOn(Schedulers.boundedElastic())
                .concatMap(tick -> executeActionAndRules(action, rules)
                        .then(reservationService.updateHeartbeat(jobId, this.instanceId, LocalDateTime.now()))
                        .onErrorResume(throwable -> {
                            log.error("Error in action execution: {}", throwable.getMessage());
                            return Mono.empty();
                        })
                )
                .onErrorResume(throwable -> {
                    log.error("Error in interval pipeline: {}", throwable.getMessage());
                    return Flux.empty();
                })
                .subscribe();

        // Store subscription information
        JobsDTO jobsDTO = new JobsDTO(
                actionEntity.getId(),
                actionEntity.getType(),
                action.getExposedParamsJson(),
                actionEntity.getRules().stream().map(ruleMapper::toDTO).toList()
        );

        subscriptions.put(actionEntity.getId(), new JobDescription(subscription, jobId, jobsDTO));
    }

    public Mono<Void> stopAction(Long actionId) {
        // Retrieve the job description
        JobDescription jobDescription = subscriptions.get(actionId);

        // If no job found, return completed Mono
        if (jobDescription == null) {
            return Mono.empty();
        }

        // Dispose the subscription if it exists
        Disposable subscription = jobDescription.getDisposable();
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }

        // Remove from subscriptions map
        subscriptions.remove(actionId);

        // Release the job and return the reactive result
        return reservationService.releaseJob(jobDescription.getJobId(), this.instanceId)
                .doOnError(error -> log.error("Error releasing job {}: {}",
                        jobDescription.getJobId(), error.getMessage(), error))
                .onErrorResume(error -> Mono.empty());  // Continue even if release fails
    }

    public boolean isActionRunning(Long actionId) {
        // Check if the actionId has an active subscription
        return subscriptions.containsKey(actionId);
    }

    public Mono<ActionEntity> create(ActionDTO action, Object authentication) {
        // Extract user ID from authentication context
        Long userId = authentication instanceof SecurityAccount sc ? sc.getAccount().getId() : null;

        if (userId == null) {
            return Mono.error(new RuntimeException("User ID is null"));
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
                .onErrorResume(ex -> {
                    // More detailed error logging
                    log.error("Failed to create action: {}", ex.getMessage(), ex);
                    return Mono.error(new RuntimeException("Failed to create action", ex));
                });
    }

    public Mono<ActionEntity> update(ActionDTO action) {
        return this.stopAction(action.getId())  // Now returns Mono<Void> directly
                .then(actionService.update(action))
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to update action", e)));
    }


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

    public Flux<JobsDTO> getJobs() {
        return Flux.fromIterable(subscriptions.values())
                .map(JobDescription::getJob);
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class JobDescription {
        private Disposable disposable;
        private Long jobId;
        private JobsDTO job;
    }
}
