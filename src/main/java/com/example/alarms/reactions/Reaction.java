package com.example.alarms.reactions;

import com.example.alarms.dto.Notification;

public interface Reaction {
    Long getRuleId();
    void execute(Notification notification);
}
