package com.example.alarms.dto;

public interface ActionWithRulesProjection {
    Long getActionId();
    String getActionType();
    String getActionParams();
    Long getRuleId();
    String getRuleName();
    String getRule();
}