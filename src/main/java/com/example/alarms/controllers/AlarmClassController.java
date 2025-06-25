package com.example.alarms.controllers;

import com.example.alarms.dto.AlarmClass;
import com.example.alarms.services.AlarmClassService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/alarm-classes")
public class AlarmClassController {

    private final AlarmClassService service;

    public AlarmClassController(AlarmClassService service) {
        this.service = service;
    }

    @PostMapping
    public Mono<AlarmClass> create(@RequestBody AlarmClass dto) {
        return service.create(dto);
    }

    @GetMapping("/{id}")
    public Mono<AlarmClass> getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    public Flux<AlarmClass> getAll() {
        return service.getAll();
    }

    @PutMapping("/{id}")
    public Mono<AlarmClass> update(@PathVariable Long id, @RequestBody AlarmClass dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> delete(@PathVariable Long id) {
        return service.delete(id);
    }
}
