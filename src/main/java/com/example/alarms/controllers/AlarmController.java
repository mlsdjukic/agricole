package com.example.alarms.controllers;
import com.example.alarms.dto.Alarm;
import com.example.alarms.dto.AlarmWithTypeAndClass;
import com.example.alarms.exceptions.EntityNotFoundException;
import com.example.alarms.services.AlarmService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    public Mono<Alarm> create(@RequestBody Alarm Alarm) {
        return alarmService.save(Alarm);
    }

    @PutMapping("/{id}")
    public Mono<Alarm> update(@PathVariable Long id, @RequestBody Alarm alarm) {
        return alarmService.update(alarm, id);
    }

    // Get alarm by ID
    @GetMapping(path = "/{id}")
    public Mono<AlarmWithTypeAndClass> getAlarmById(@PathVariable Long id) {
        return alarmService.getAlarmWithTypeAndClass(id);
    }

    // Delete alarm by ID
    @DeleteMapping(path = "/{id}")
    public Mono<Void> deleteAlarm(@PathVariable Long id) {
        return alarmService.deleteById(id);
    }

    // Get the last record by ruleId
    @GetMapping(path = "/last/{ruleId}")
    public Mono<Alarm> getLastRecordByRuleId(@PathVariable Long ruleId) {
        return alarmService.getLastRecordByRuleId(ruleId);
    }

    // Get all alarms with pagination
    @GetMapping
    public Flux<Alarm> getAlarmsPage(@RequestParam int page, @RequestParam int size) {
        return alarmService.getAllWithPagination(page, size);
    }

    @GetMapping("/all")
    public Flux<Alarm> getAllAlarms() {
        return alarmService.getAll();
    }

}
