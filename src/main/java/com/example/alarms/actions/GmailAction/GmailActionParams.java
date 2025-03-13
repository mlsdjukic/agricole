package com.example.alarms.actions.GmailAction;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data; /**
 * Gmail action parameters
 */
@Data
@Schema(description = "Parameters for Gmail action")
public class GmailActionParams {
    @Schema(description = "Google username",
            example = "123456789-abc123def456.apps.googleusercontent.com",
            required = true)
    @JsonProperty("username")
    private String username;

    @Schema(description = "Google password",
            example = "GOCSPX-abc123def456",
            required = true)
    @JsonProperty("password")
    private String password;

    @Schema(description = "Interval in minutes", example = "5", minimum = "1")
    @JsonProperty("interval")
    private Long interval;
}
