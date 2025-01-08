package com.example.alarms.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ActionDTO {
    private Long id;
    private String type;
    private Map<String,Object> params;
    private List<RuleDTO> rules;

}

