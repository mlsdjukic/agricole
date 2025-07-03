package com.example.alarms.reactions;

import com.example.alarms.components.ApplicationContextProvider;
import com.example.alarms.dto.AlarmRequest;
import com.example.alarms.dto.Notification;
import com.example.alarms.services.AlarmService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WriteAlarmToDBReaction implements Reaction{

    private final Long ruleId;

    private final AlarmService alarmService;

    public WriteAlarmToDBReaction(Long ruleId) {
        this.ruleId = ruleId;
        this.alarmService = ApplicationContextProvider.getApplicationContext().getBean(AlarmService.class);
    }

    @Override
    public Long getRuleId() {
        return this.ruleId;
    }

    @Override
    public void execute(Notification notification) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(notification);
            AlarmRequest alarm = new AlarmRequest();
            alarm.setRuleId(notification.getRuleId());
            alarm.setAlarmTypeId(notification.getAlarmTypeId());
            alarm.setAlarmClassId(notification.getAlarmClassId());
            alarm.setMessage(jsonString);
            alarmService.save(alarm)
                    .subscribe();
        } catch (Exception e) {
            log.error("Failed to write alarm into database");
        }
    }
}
