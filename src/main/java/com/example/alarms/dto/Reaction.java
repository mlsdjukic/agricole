package com.example.alarms.dto;

import com.example.alarms.reactions.SendEmailReaction.SendEmailReactionParams;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data; /**
 * Reaction model
 */
@Data
@Schema(description = "Definition of a reaction to a rule match")
public class Reaction {
    @Schema(description = "The type of reaction to trigger",
            example = "SendEmailReaction",
            allowableValues = {"SendEmailReaction"},
            required = true)
    private String name;

    @Schema(
            description = "Parameters specific to the reaction type",
            required = true,
            oneOf = {
                    SendEmailReactionParams.class
            }
    )
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
            property = "name"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = SendEmailReactionParams.class, name = "SendEmailReaction")
    })
    private Object params;
}
