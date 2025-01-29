package com.example.alarms.reactions;

import com.example.alarms.dto.NotificationDTO;

public interface Reaction {
    Long getRuleId();
    void execute(NotificationDTO notification);
}
