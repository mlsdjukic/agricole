package com.example.alarms.actions;

import reactor.core.publisher.Flux;

public interface Action {
    Flux<Object> execute();
    Long getInterval();
}
