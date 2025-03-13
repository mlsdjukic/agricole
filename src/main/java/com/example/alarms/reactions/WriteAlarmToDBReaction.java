package com.example.alarms.reactions;

import com.example.alarms.components.ApplicationContextProvider;
import com.example.alarms.dto.AlarmRequest;
import com.example.alarms.dto.Notification;
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
    public void execute(Notification notification) {
        alarmService.save(new AlarmRequest(notification.getRuleId(), notification.getMessage()))
                .subscribe();
    }
}
