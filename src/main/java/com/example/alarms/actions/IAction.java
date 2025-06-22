package com.example.alarms.actions;

import reactor.core.publisher.Flux;

import java.util.Map;

public interface IAction {
    Flux<Object> execute();
    Long getInterval();
    Map<String, Object> getExposedParamsJson();
}
