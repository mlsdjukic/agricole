package com.example.alarms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JobsDTO {
    private Long actionId;
    private String type;
    private String params;
    private List<RuleDTO> rules;

}
