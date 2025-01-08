package com.example.alarms.controllers;

import com.example.alarms.components.JobCoordinator;
import com.example.alarms.dto.ActionDTO;
import com.example.alarms.services.ActionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
public class JobController {

    private final JobCoordinator jobCoordinator;

    public JobController(JobCoordinator jobCoordinator) {
        this.jobCoordinator = jobCoordinator;
    }

    @PostMapping(value = "/", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<ResponseEntity<Void>> create(@RequestBody ActionDTO action) {

        return jobCoordinator.create(action).thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

    }

    @PutMapping(value = "/", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<ResponseEntity<Void>> update(@RequestBody ActionDTO action) {

        if (action.getId() == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Action ID cannot be null"));
        }

        return jobCoordinator.update(action).thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

    }


    @DeleteMapping(value = "/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {

        if (id == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Action ID cannot be null"));
        }

        return jobCoordinator.delete(id).thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT).build());

    }

}
