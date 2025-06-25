package com.example.alarms.dto;

import com.example.alarms.entities.AlarmClassEntity;
import com.example.alarms.entities.AlarmEntity;
import com.example.alarms.entities.AlarmTypeEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlarmWithTypeAndClass {
    private AlarmEntity alarm;
    private AlarmTypeEntity type;
    private AlarmClassEntity alarmClass;

}
