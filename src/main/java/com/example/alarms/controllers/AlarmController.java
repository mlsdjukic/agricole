package com.example.alarms.controllers;
import com.example.alarms.dto.AlarmRequestDTO;
import com.example.alarms.dto.AlarmResponseDTO;
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
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<AlarmResponseDTO> createOrUpdateAlarm(@RequestBody AlarmRequestDTO AlarmRequestDTO) {
        return alarmService.save(AlarmRequestDTO);
    }

    // Get alarm by ID
    @GetMapping(path = "/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<AlarmResponseDTO> getAlarmById(@PathVariable Long id) {
        return alarmService.getById(id);
    }

    // Delete alarm by ID
    @DeleteMapping(path = "/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<Void> deleteAlarm(@PathVariable Long id) {
        return alarmService.deleteById(id);
    }

    // Get the last record by ruleId
    @GetMapping(path = "/last/{ruleId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<AlarmResponseDTO> getLastRecordByRuleId(@PathVariable Long ruleId) {
        return alarmService.getLastRecordByRuleId(ruleId);
    }

    // Get all alarms with pagination
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AlarmResponseDTO> getAlarmsPage(@RequestParam int page, @RequestParam int size) {
        return alarmService.getAllWithPagination(page, size);
    }

    @GetMapping("/all")
    public Flux<AlarmResponseDTO> getAllAlarms() {
        return alarmService.getAll();
    }
}
