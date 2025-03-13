package com.example.alarms.reactions.SendEmailReaction;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data; /**
 * SendEmailReaction parameters
 */
@Data
@Schema(description = "Parameters for SendEmailReaction")
public class SendEmailReactionParams {
    @JsonProperty("email_address")
    @Schema(description = "Email address to send to", example = "user@example.com", required = true)
    private String emailAddress;
}
