package com.example.alarms.components;

import com.example.alarms.actions.Action;
import com.example.alarms.dto.ActionDTO;
import com.example.alarms.dto.JobsDTO;
import com.example.alarms.dto.RuleMapper;
import com.example.alarms.entities.ActionEntity;
import com.example.alarms.entities.ReactionEntity;
import com.example.alarms.entities.RuleEntity;
import com.example.alarms.entities.security.SecurityAccount;
import com.example.alarms.reactions.Reaction;
import com.example.alarms.reactions.WriteAlarmToDBReaction;
import com.example.alarms.rules.Rule;
import com.example.alarms.services.ActionService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.PostConstruct;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.env.Environment;
@Component
public class JobCoordinator {

    private  final RuleMapper ruleMapper;
    private final ActionService actionService;

    private final Map<Long, JobDescription> subscriptions = new ConcurrentHashMap<>();

    private final Integer limit;
    private final Integer offset;


    public JobCoordinator(ActionService actionService, RuleMapper ruleMapper, Environment env) {
        this.actionService = actionService;
        this.ruleMapper = ruleMapper;

        this.limit = Integer.parseInt(env.getProperty("LIMIT", "100"));
        this.offset = Integer.parseInt(env.getProperty("OFFSET", "0"));
    }

    @PostConstruct
    public void initializeJobs() {
        // Fetch jobs from the database
        actionService.findActionsInRange(this.limit, this.offset)
                .doOnNext(this::scheduleJob) // Schedule each job
                .subscribe();
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


    private void scheduleJob(ActionEntity actionEntity) {
        System.out.println("Scheduling job: " + actionEntity.getType());
        Action newAction = null;
        List<Rule> newRules = new ArrayList<>();
        List<Reaction> newReactions = new ArrayList<>();

        try {
            newAction = createAction(actionEntity);
            newRules = createRules(actionEntity.getRules());
        } catch (Exception e) {
            Throwable original = e.getCause(); // Unwraps the target
            if (original != null) {
                System.out.println("Failed to create Action with error: " + original.getMessage());
            } else {
                System.out.println("Caught Exception: " + e.getMessage());
            }

            return;
        }

        if (newAction != null) {
            Action finalNewAction = newAction;
            List<Rule> finalNewRules = newRules;

            finalNewAction.execute()
                    .flatMap(data -> Flux.fromIterable(finalNewRules)
                            .doOnNext(rule -> rule.execute(data)) // Execute each rule with the data
                            .then() // Complete after all rules are executed
                    ).subscribe();

            Disposable subscription = Flux.interval(Duration.ofSeconds(newAction.getInterval())) // Emit events periodically
                    .publishOn(Schedulers.boundedElastic()) // Use a bounded thread pool for execution
                    .concatMap(tick -> finalNewAction.execute()
                            .flatMap(data -> Flux.fromIterable(finalNewRules)
                                    .doOnNext(rule -> rule.execute(data)) // Execute each rule with the data
                                    .then() // Complete after all rules are executed
                            )
                    ) // Ensure sequential execution per interval
                    .subscribe(); // Activate the pipeline

            subscriptions.put(actionEntity.getId(), new JobDescription(subscription, new JobsDTO(
                    actionEntity.getId(),
                    actionEntity.getType(),
                    newAction.getExposedParamsJson(),
                    actionEntity.getRules().stream().map(ruleMapper::toDTO).toList()

            )));
        }
    }


    public void stopAction(Long actionId) {
        // Retrieve the subscription and dispose it if it exists
        JobDescription jobDescription = subscriptions.get(actionId);
        if (jobDescription != null) {
            Disposable subscription = jobDescription.getDisposable();
            if (subscription != null) {
                subscription.dispose();
                subscriptions.remove(actionId); // Optionally, remove the entry from the map after disposing
            }
        }
    }

    public boolean isActionRunning(Long actionId) {
        // Check if the actionId has an active subscription
        return subscriptions.containsKey(actionId);
    }

    public Mono<ActionEntity> create(ActionDTO action, Object authentication){
        Long userId = null;
        if (authentication instanceof SecurityAccount sc){
            userId = sc.getAccount().getId();
        }

        if (userId == null){
            return Mono.error(new RuntimeException("User ID is null"));
        }
        return actionService.create(action, userId)
                .flatMap(actionEntity -> {
                    this.scheduleJob(actionEntity);
                    return Mono.just(actionEntity);
                })
                .onErrorResume(ex -> Mono.error(new RuntimeException("Failed to create action", ex)));

    }

    public Mono<ActionEntity> update(ActionDTO action) {
        return Mono.fromRunnable(() -> this.stopAction(action.getId()))  // Ensures non-blocking execution
                .then(actionService.update(action)) // Continues execution if stopAction succeeds
                .flatMap(actionEntity -> {
                    this.scheduleJob(actionEntity);
                    return Mono.just(actionEntity);
                })
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to update action", e)));
    }


    public Mono<Void> delete(Long actionId){
        this.stopAction(actionId);
        return actionService.deleteAction(actionId)
                .onErrorResume(ex -> Mono.error(new RuntimeException("Failed to delete action", ex)));



    }

    public Flux<JobsDTO> getJobs() {
        return actionService.getAllActions()
                .flatMap(actionEntity -> {
                        JobDescription jobDescription = subscriptions.get(actionEntity.getId());
                        return Mono.just(jobDescription.getJob());
                })
                .onErrorResume(ex -> Mono.error(new RuntimeException(ex)));

    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class JobDescription {
        private Disposable disposable;
        private JobsDTO job;
    }
}
