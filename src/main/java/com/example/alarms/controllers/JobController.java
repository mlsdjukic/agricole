package com.example.alarms.controllers;

import com.example.alarms.components.JobCoordinator;
import com.example.alarms.dto.ActionDTO;
import com.example.alarms.dto.JobsDTO;
import com.example.alarms.services.ActionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequestMapping("/jobs")
@RestController
@SecurityRequirement(name = "basicAuth")
public class JobController {

    private final JobCoordinator jobCoordinator;

    public JobController(JobCoordinator jobCoordinator) {
        this.jobCoordinator = jobCoordinator;
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<ResponseEntity<Void>> create(@RequestBody ActionDTO action) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .flatMap(authentication -> jobCoordinator.create(action, authentication))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());
    }

    @PutMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
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


    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<JobsDTO> get() {
        return jobCoordinator.getJobs();
    }
}


