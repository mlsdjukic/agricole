package com.example.alarms.components;

import com.example.alarms.actions.Action;
import com.example.alarms.dto.ActionDTO;
import com.example.alarms.entities.ActionEntity;
import com.example.alarms.entities.RuleEntity;
import com.example.alarms.entities.security.SecurityAccount;
import com.example.alarms.rules.Rule;
import com.example.alarms.services.ActionService;
import com.example.alarms.services.AlarmService;
import org.springframework.context.annotation.DependsOn;
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

import org.springframework.core.env.Environment;
@DependsOn("liquibase")
@Component
public class JobCoordinator {


    private final ActionService actionService;

    private final Map<Long, Disposable> subscriptions = new ConcurrentHashMap<>();

    private final Integer limit;
    private final Integer offset;

    private final AlarmService alarmService;

    public JobCoordinator(ActionService actionService, AlarmService alarmService, Environment env, AlarmService alarmService1) {
        this.actionService = actionService;

        this.limit = Integer.parseInt(env.getProperty("LIMIT", "100"));
        this.offset = Integer.parseInt(env.getProperty("OFFSET", "0"));
        this.alarmService = alarmService1;
    }

    @PostConstruct
    public void initializeJobs() {
        // Fetch jobs from the database
        actionService.findActionsInRange(this.limit, this.offset)
                .doOnNext(this::scheduleJob) // Schedule each job
                .subscribe();
    }

    public Action createAction(ActionEntity actionEntity) {
        try {
            String actionClassName = "com.example.alarms.actions." + actionEntity.getType();
            return (Action) createInstance(actionClassName, new Class<?>[]{String.class, Long.class}, actionEntity.getParams(), actionEntity.getId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Action: " + actionEntity.getType(), e);
        }
    }

    public List<Rule> createRules(List<RuleEntity> ruleEntities) {
        return ruleEntities.stream()
                .map(rule -> {
                    try {
                        String ruleClassName = "com.example.alarms.rules." + rule.getName();
                        return (Rule) createInstance(ruleClassName, new Class<?>[]{String.class, Long.class, AlarmService.class}, rule.getRule(), rule.getId(), this.alarmService);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create Rule: " + rule.getName(), e);
                    }
                })
                .collect(Collectors.toList());
    }

    private Object createInstance(String className, Class<?>[] parameterTypes, Object... args) throws Exception {
        Class<?> clazz = Class.forName(className);
        Constructor<?> constructor = clazz.getDeclaredConstructor(parameterTypes);
        return constructor.newInstance(args);
    }


    private void scheduleJob(ActionEntity actionEntity) {
        System.out.println("Scheduling job: " + actionEntity.getType());
        Action newAction = null;
        List<Rule> newRules = new ArrayList<Rule>();

        try {
            newAction = createAction(actionEntity);
            newRules = createRules(actionEntity.getRules());


        } catch (Exception e) {
            e.printStackTrace(); // Handle the exception or log it appropriately
        }

        if (newAction != null) {
            Action finalNewAction = newAction;
            List<Rule> finalNewRules = newRules;

            finalNewAction.execute()
                    .flatMap(data -> Flux.fromIterable(finalNewRules)
                            .doOnNext(rule -> rule.execute(data)) // Execute each rule with the data
                            .then() // Complete after all rules are executed
                    );

            Disposable subscription = Flux.interval(Duration.ofMillis(newAction.getInterval())) // Emit events periodically
                    .publishOn(Schedulers.boundedElastic()) // Use a bounded thread pool for execution
                    .concatMap(tick -> finalNewAction.execute()
                            .flatMap(data -> Flux.fromIterable(finalNewRules)
                                    .doOnNext(rule -> rule.execute(data)) // Execute each rule with the data
                                    .then() // Complete after all rules are executed
                            )
                    ) // Ensure sequential execution per interval
                    .subscribe(); // Activate the pipeline

            subscriptions.put(actionEntity.getId(), subscription);
        }
    }


    public void stopAction(Long actionId) {
        // Retrieve the subscription and dispose it if it exists
        Disposable subscription = subscriptions.get(actionId);
        if (subscription != null) {
            subscription.dispose();
            subscriptions.remove(actionId); // Optionally, remove the entry from the map after disposing
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
        return actionService.createWithRules(action, userId)
                .flatMap(actionEntity -> {
                    this.scheduleJob(actionEntity);
                    return Mono.just(actionEntity);
                });

    }

    public Mono<ActionEntity> update(ActionDTO action){
        this.stopAction(action.getId());
        return actionService.update(action)
                .flatMap(actionEntity -> {;
                    this.scheduleJob(actionEntity);
                    return Mono.just(actionEntity);
                });

    }

    public Mono<Void> delete(Long actionId){
        this.stopAction(actionId);
        return actionService.delete(actionId);


    }
}
