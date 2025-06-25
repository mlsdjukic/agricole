package com.example.alarms.controllers;

import com.example.alarms.dto.AlarmType;
import com.example.alarms.services.AlarmTypeService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/alarm-types")
public class AlarmTypeController {

    private final AlarmTypeService service;

    public AlarmTypeController(AlarmTypeService service) {
        this.service = service;
    }

    @PostMapping
    public Mono<AlarmType> create(@RequestBody AlarmType dto) {
        return service.create(dto);
    }

    @GetMapping("/{id}")
    public Mono<AlarmType> getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    public Flux<AlarmType> getAll() {
        return service.getAll();
    }

    @PutMapping("/{id}")
    public Mono<AlarmType> update(@PathVariable Long id, @RequestBody AlarmType dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> delete(@PathVariable Long id) {
        return service.delete(id);
    }
}

