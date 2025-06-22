package com.example.alarms.controllers;

import com.example.alarms.dto.AlarmRequest;
import com.example.alarms.dto.AlarmResponse;
import com.example.alarms.services.AlarmService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<AlarmResponse> createOrUpdateAlarm(@RequestBody AlarmRequest alarmRequest) {
        AlarmResponse response = alarmService.save(alarmRequest);
        return ResponseEntity.ok(response);
    }

    // Get alarm by ID
    @GetMapping("/{id}")
    public ResponseEntity<AlarmResponse> getAlarmById(@PathVariable Long id) {
        return alarmService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Delete alarm by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlarm(@PathVariable Long id) {
        alarmService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Get the last record by ruleId
    @GetMapping("/last/{ruleId}")
    public ResponseEntity<AlarmResponse> getLastRecordByRuleId(@PathVariable Long ruleId) {
        return alarmService.getLastRecordByRuleId(ruleId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Get all alarms with pagination
    @GetMapping
    public ResponseEntity<List<AlarmResponse>> getAlarmsPage(
            @RequestParam int page,
            @RequestParam int size
    ) {
        return ResponseEntity.ok(alarmService.getAllWithPagination(page, size));
    }

    // Get all alarms
    @GetMapping("/all")
    public ResponseEntity<List<AlarmResponse>> getAllAlarms() {
        return ResponseEntity.ok(alarmService.getAll());
    }
}
