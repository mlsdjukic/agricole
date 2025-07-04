package com.example.alarms.dto;

import com.example.alarms.actions.EwsAction.EwsActionParams;
import com.example.alarms.actions.GmailAction.GmailActionParams;
import com.example.alarms.entities.ActionEntity;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class ActionResponse {
    private Long id;
    @Schema(description = "The type of action to create",
            example = "EwsAction",
            allowableValues = {"EwsAction", "GmailAction"},
            required = true)
    private String type;

    @Schema(
            description = "Parameters specific to the action type",
            required = true,
            oneOf = {
                    EwsActionParams.class,
                    GmailActionParams.class
            }
    )
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
            property = "type"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = EwsActionParams.class, name = "EwsAction"),
            @JsonSubTypes.Type(value = GmailActionParams.class, name = "GmailAction")
    })
    private Object params;

    @Schema(description = "The type of alarm that this action will produce",
            example = "System")
    private Long alarmTypeId;

    @Schema(description = "The class of alarm that this action will produce",
            example = "Red")
    private Long alarmClassId;

    @Schema(description = "Rules to apply for this action", required = true)
    private List<Rule> rules;

}