package com.example.alarms.reactions;

import com.example.alarms.components.ApplicationContextProvider;
import com.example.alarms.dto.AlarmRequestDTO;
import com.example.alarms.dto.NotificationDTO;
import com.example.alarms.services.AlarmService;

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
    public void execute(NotificationDTO notification) {
        alarmService.save(new AlarmRequestDTO(notification.getRuleId(), notification.getMessage()))
                .subscribe();
    }
}
