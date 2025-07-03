package com.example.alarms.controllers;
import com.example.alarms.dto.AlarmRequest;
import com.example.alarms.dto.AlarmResponse;
import com.example.alarms.dto.AlarmWithTypeAndClass;
import com.example.alarms.entities.security.SecurityAccount;
import com.example.alarms.exceptions.InvalidActionException;
import com.example.alarms.exceptions.RuleProcessingException;
import com.example.alarms.exceptions.SerializationException;
import com.example.alarms.exceptions.UserNotFoundException;
import com.example.alarms.services.AlarmService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/alarms")
@SecurityRequirement(name = "basicAuth")
public class AlarmController {

    private final AlarmService alarmService;

    public AlarmController(AlarmService alarmService) {
        this.alarmService = alarmService;
    }

    // Create or update an alarm
    @PostMapping
    public Mono<AlarmResponse> create(@RequestBody AlarmRequest AlarmRequest) {
        return alarmService.save(AlarmRequest);
    }

    @PutMapping("/{id}")
    public Mono<AlarmResponse> update(@PathVariable Long id, @RequestBody AlarmRequest alarmRequest) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .flatMap(authentication -> {
                    Long userId = authentication instanceof SecurityAccount sc ? sc.getAccount().getId() : null;

                    if (userId == null) {
                        return Mono.error(new UserNotFoundException("User ID is null"));
                    }

                    return alarmService.update(alarmRequest, id, userId);
                })
                .onErrorMap(e -> {
                    switch (e) {
                        case UserNotFoundException userNotFoundException -> {
                            return new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
                        }
                        case InvalidActionException invalidActionException -> {
                            return new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
                        }
                        case RuleProcessingException ruleProcessingException -> {
                            return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e);
                        }
                        case SerializationException serializationException -> {
                            return new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
                        }
                        case DuplicateKeyException duplicateKeyException -> {
                            return new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
                        }
                        case null, default -> {
                            log.error("Unexpected error during action creation", e);
                            return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                    "An error occurred while creating the action", e);
                        }
                    }
                });
    }

    // Get alarm by ID
    @GetMapping(path = "/{id}")
    public Mono<AlarmWithTypeAndClass> getAlarmById(@PathVariable Long id) {
        return alarmService.getAlarmWithTypeAndClass(id);
    }

    // Delete alarm by ID
    @DeleteMapping(path = "/{id}")
    public Mono<Void> delete(@PathVariable Long id) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .flatMap(authentication -> {
                    Long userId = authentication instanceof SecurityAccount sc ? sc.getAccount().getId() : null;

                    if (userId == null) {
                        return Mono.error(new UserNotFoundException("User ID is null"));
                    }

                    return alarmService.deleteById(id, userId);
                })
                .onErrorMap(e -> {
                    switch (e) {
                        case UserNotFoundException userNotFoundException -> {
                            return new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
                        }
                        case InvalidActionException invalidActionException -> {
                            return new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
                        }
                        case RuleProcessingException ruleProcessingException -> {
                            return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e);
                        }
                        case SerializationException serializationException -> {
                            return new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
                        }
                        case DuplicateKeyException duplicateKeyException -> {
                            return new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
                        }
                        case null, default -> {
                            log.error("Unexpected error during action creation", e);
                            return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                    "An error occurred while creating the action", e);
                        }
                    }
                });
    }

    // Get all alarms with pagination
    @GetMapping
    public Flux<AlarmResponse> getAlarmsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        // Validate pagination parameters
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page index must be greater than or equal to 0");
        }
        if (size <= 0 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be between 1 and 100");
        }

        // Create pagination object
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
                        sortBy != null ? sortBy : "id")
        );

        return alarmService.getAllWithPagination(pageable);
    }

    @GetMapping("/all")
    public Flux<AlarmWithTypeAndClass> getAllAlarms() {
        return alarmService.getAll();
    }

}
