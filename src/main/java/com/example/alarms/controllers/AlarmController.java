package com.example.alarms.controllers;
import com.example.alarms.dto.AlarmRequestDTO;
import com.example.alarms.dto.AlarmResponseDTO;
import com.example.alarms.services.AlarmService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/alarms")
public class AlarmController {

    private final AlarmService alarmService;

    public AlarmController(AlarmService alarmService) {
        this.alarmService = alarmService;
    }

    // Create or update an alarm
    @PostMapping
    public Mono<AlarmResponseDTO> createOrUpdateAlarm(@RequestBody AlarmRequestDTO AlarmRequestDTO) {
        return alarmService.save(AlarmRequestDTO);
    }

    // Get alarm by ID
    @GetMapping("/{id}")
    public Mono<AlarmResponseDTO> getAlarmById(@PathVariable Long id) {
        return alarmService.getById(id);
    }

    // Delete alarm by ID
    @DeleteMapping("/{id}")
    public Mono<Void> deleteAlarm(@PathVariable Long id) {
        return alarmService.deleteById(id);
    }

    // Get the last record by ruleId
    @GetMapping("/last/{ruleId}")
    public Mono<AlarmResponseDTO> getLastRecordByRuleId(@PathVariable Long ruleId) {
        return alarmService.getLastRecordByRuleId(ruleId);
    }

    // Get all alarms with pagination
    @GetMapping
    public Flux<AlarmResponseDTO> getAllAlarms(@RequestParam int page, @RequestParam int size) {
        return alarmService.getAllWithPagination(page, size);
    }
}
