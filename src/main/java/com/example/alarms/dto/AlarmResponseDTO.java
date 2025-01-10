package com.example.alarms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlarmResponseDTO {
    private Long id;
    private Long ruleId;
    private String message;
}

