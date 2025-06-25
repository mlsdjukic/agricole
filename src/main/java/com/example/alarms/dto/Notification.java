package com.example.alarms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Notification {

    private Long ruleId;
    private String message;
    private String body;
    private String sender;
    private String subject;
    private Long alarmTypeId;
    private Long alarmClassId;
    private String status;
    private String note;
}
