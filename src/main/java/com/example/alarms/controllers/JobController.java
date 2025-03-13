package com.example.alarms.controllers;

import com.example.alarms.components.Coordinator;
import com.example.alarms.dto.*;
import com.example.alarms.exceptions.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequestMapping("/jobs")
@RestController
@SecurityRequirement(name = "basicAuth")
public class JobController {

    private final Coordinator coordinator;
    private final ActionMapper actionMapper;

    public JobController(Coordinator coordinator, AccountMapper accountMapper, ActionMapper actionMapper) {
        this.coordinator = coordinator;
        this.actionMapper = actionMapper;
    }


    @Operation(
            summary = "Create a new action",
            description = "Creates a new action with specific parameters and rules",
            operationId = "createAction",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ActionRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Action created successfully",
                            content = @Content(
                                    mediaType = "text/event-stream",
                                    schema = @Schema(implementation = ActionResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request body",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error"
                    )
            }
    )
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseStatus(HttpStatus.CREATED) // This sets the default success status
    public Mono<ActionResponse> create(@RequestBody ActionRequest action) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .flatMap(authentication -> coordinator.create(action, authentication))
                .map(actionMapper::toActionResponse)
                .onErrorMap(e -> {
                    if (e instanceof UserNotFoundException) {
                        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
                    } else if (e instanceof InvalidActionException) {
                        return new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
                    } else if (e instanceof RuleProcessingException) {
                        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e);
                    } else if (e instanceof SerializationException) {
                        return new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
                    } else if (e instanceof DuplicateKeyException) {
                        return new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
                    } else {
                        log.error("Unexpected error during action creation", e);
                        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "An error occurred while creating the action", e);
                    }
                });
    }

    @Operation(
            summary = "Create a new action",
            description = "Creates a new action with specific parameters and rules",
            operationId = "createAction",
            parameters = {
                    @Parameter(
                            name = "id",
                            description = "Unique identifier for the action",
                            in = ParameterIn.PATH,
                            required = true,
                            schema = @Schema(type = "string")
                    )
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ActionRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Action created successfully",
                            content = @Content(
                                    mediaType = "text/event-stream",
                                    schema = @Schema(implementation = ActionResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request body",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error"
                    )
            }
    )
    @PutMapping(value = "/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<ActionResponse> update(@PathVariable Long id, @RequestBody ActionRequest action) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID cannot be null");
        }

        return coordinator.update(action, id)
                .map(actionMapper::toActionResponse)
                .onErrorMap(e -> {
                    if (e instanceof UserNotFoundException) {
                        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
                    } else if (e instanceof InvalidActionException) {
                        return new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
                    } else if (e instanceof EntityNotFoundException) {
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
                    } else if (e instanceof RuleProcessingException) {
                        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e);
                    } else if (e instanceof SerializationException) {
                        return new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
                    } else if (e instanceof DuplicateKeyException) {
                        return new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
                    } else {
                        log.error("Unexpected error during action update for ID {}", id, e);
                        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "An error occurred while updating the action", e);
                    }
                });
    }


    @DeleteMapping(value = "/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<Void> delete(@PathVariable Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID cannot be null");
        }

        return coordinator.delete(id)
                .onErrorMap(e -> {
                    if (e instanceof EntityNotFoundException) {
                        log.warn("Action with ID {} not found for deletion", id);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Action not found with ID: " + id, e);
                    } else {
                        log.error("Unexpected error during action deletion for ID {}", id, e);
                        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "Failed to delete action with ID: " + id, e);
                    }
                });
    }


    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ActionResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
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

        return coordinator.get(pageable)
                .map(actionMapper::toActionResponse)
                .onErrorMap(e -> {
                    log.error("Error retrieving actions: {}", e.getMessage(), e);

                    // Map different exceptions to appropriate HTTP status exceptions
                    if (e instanceof IllegalArgumentException) {
                        return new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
                    } else if (e instanceof EntityNotFoundException) {
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
                    }  else if (e instanceof DataAccessException) {
                        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Database service unavailable", e);
                    } else {
                        // For unexpected errors
                        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", e);
                    }
                });
    }

    @GetMapping(value = "/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<ActionResponse> get(@PathVariable Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page index must be greater than or equal to 0");
        }

        return coordinator.get(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Action not found with ID: " + id)))
                .map(actionMapper::toActionResponse)
                .onErrorMap(e -> {
                    log.error("Error retrieving actions: {}", e.getMessage(), e);

                    if (e instanceof ResponseStatusException) {
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
                    }
                    // Map different exceptions to appropriate HTTP status exceptions
                    if (e instanceof IllegalArgumentException) {
                        return new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
                    } else if (e instanceof EntityNotFoundException) {
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
                    }  else if (e instanceof DataAccessException) {
                        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Database service unavailable", e);
                    } else {
                        // For unexpected errors
                        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", e);
                    }
                });
    }
}


