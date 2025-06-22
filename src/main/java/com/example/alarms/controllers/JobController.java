package com.example.alarms.controllers;

import com.example.alarms.components.Coordinator;
import com.example.alarms.dto.*;
import com.example.alarms.entities.ActionEntity;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequestMapping("/jobs")
@RestController
@SecurityRequirement(name = "basicAuth")
public class JobController {

    private final Coordinator coordinator;
    private final ActionMapper actionMapper;

    public JobController(Coordinator coordinator, ActionMapper actionMapper) {
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
                            schema = @Schema(implementation = Action.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Action created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Action.class)
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
    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED) // This sets the default success status
    public ResponseEntity<Action> create(@RequestBody Action action, Authentication authentication) {
        try {

            // Call imperative version of create (assumed to return Action)
            ActionEntity createdAction = coordinator.create(action, authentication.getPrincipal()); // this method must be blocking

            Action response = actionMapper.toDto(createdAction);
            return ResponseEntity.status(HttpStatus.CREATED).build();

        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
        } catch (InvalidActionException | SerializationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (RuleProcessingException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e);
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during action creation", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "An error occurred while creating the action",
                    e
            );
        }
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
                            schema = @Schema(implementation = Action.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Action created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Action.class)
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
    @PutMapping("/{id}")
    public ResponseEntity<Action> update(@PathVariable Long id, @RequestBody Action action) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID cannot be null");
        }

        try {
            ActionEntity updated = coordinator.update(action, id); // must be imperative
            Action response = actionMapper.toDto(updated);
            return ResponseEntity.ok(response);

        } catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
        } catch (InvalidActionException | SerializationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (RuleProcessingException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e);
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during action update for ID {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "An error occurred while updating the action", e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID cannot be null");
        }

        try {
            coordinator.delete(id); // now imperative
            return ResponseEntity.noContent().build();

        } catch (EntityNotFoundException e) {
            log.warn("Action with ID {} not found for deletion", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Action not found with ID: " + id, e);

        } catch (Exception e) {
            log.error("Unexpected error during action deletion for ID {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete action with ID: " + id, e);
        }
    }



    @GetMapping
    public ResponseEntity<List<Action>> getAll(
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

        try {
            // Create pagination object
            Pageable pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by(
                            direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
                            sortBy != null ? sortBy : "id"
                    )
            );

            List<ActionEntity> entities = coordinator.get(pageable); // imperative method
            List<Action> responses = entities.stream()
                    .map(actionMapper::toDto)
                    .toList();

            return ResponseEntity.ok(responses);

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);

        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);

        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Database service unavailable", e);

        } catch (Exception e) {
            log.error("Error retrieving actions: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", e);
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<Action> get(@PathVariable Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page index must be greater than or equal to 0");
        }

        try {
            Optional<ActionEntity> optionalAction = coordinator.get(id); // now imperative

            if (optionalAction.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Action not found with ID: " + id);
            }

            Action response = actionMapper.toDto(optionalAction.get());
            return ResponseEntity.ok(response);

        } catch (ResponseStatusException e) {
            throw e;

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);

        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);

        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Database service unavailable", e);

        } catch (Exception e) {
            log.error("Error retrieving action: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", e);
        }
    }

}


