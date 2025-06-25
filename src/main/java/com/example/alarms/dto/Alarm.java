package com.example.alarms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Alarm {
    private Long id;
    private Long ruleId;
    private String message;
    private String status;
    private Boolean archived;
    private String createdFrom;
    private String metadata;
    private String relation;
    private Long alarmTypeId;
    private Long alarmClassId;
    private LocalDateTime createdDate;
    private LocalDateTime updatedAt;
}

