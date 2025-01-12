package com.example.alarms.actions;

import reactor.core.publisher.Flux;

import java.util.Map;

public interface Action {
    Flux<Object> execute();
    Long getInterval();
    Map<String, Object> getExposedParamsJson();
}
