package com.example.alarms.dto;

import com.example.alarms.rules.DetectMissingEmail.DetectMissingEmailDefinition;
import com.example.alarms.rules.FindPatternInEws.FindPatternInEwsDefinition;
import com.example.alarms.rules.FindPatternInGmail.FindPatternInGmailDefinition;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List; /**
 * Rule model
 */
@Data
@Schema(description = "Rule to apply for an action")
public class Rule {
    @Schema(description = "Name of the rule defining its behavior",
            example = "FindPatternInEws",
            allowableValues = {"FindPatternInEws","FindPatternInGmail","DetectMissingEmail"},
            required = true)
    private String name;

    @Schema(
            description = "Definition specific to the rule type",
            required = true,
            oneOf = {
                    FindPatternInEwsDefinition.class,
                    FindPatternInGmailDefinition.class,
                    DetectMissingEmailDefinition.class
            }
    )
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
            property = "name"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = FindPatternInEwsDefinition.class, name = "FindPatternInEws"),
            @JsonSubTypes.Type(value = FindPatternInGmailDefinition.class, name = "FindPatternInGmail"),
            @JsonSubTypes.Type(value = DetectMissingEmailDefinition.class, name = "DetectMissingEmail")
    })
    private Object definition;

    @Schema(description = "Reactions to trigger when rule conditions are met", required = true)
    private List<Reaction> reactions;
}