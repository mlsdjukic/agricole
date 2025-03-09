package com.example.alarms.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActionResponse {
    private ActionDTO action;
    private String error;

    // Success constructor
    public ActionResponse(ActionDTO action) {
        this.action = action;
    }

    // Error constructor
    public ActionResponse(String error) {
        this.error = error;
    }
}