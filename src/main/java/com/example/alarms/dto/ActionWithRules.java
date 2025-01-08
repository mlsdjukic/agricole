package com.example.alarms.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ActionWithRules {
    private Long actionId;
    private String actionType;
    private String actionParams;
    private Long ruleId;
    private String ruleName;
    private String rule;

}
