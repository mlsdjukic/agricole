package com.example.alarms.actions.EwsAction;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data; /**
 * EWS action parameters
 */
@Data
@Schema(description = "Parameters for EWS action")
public class EwsActionParams {
    @JsonProperty("ews_url")
    @Schema(description = "EWS URL", example = "https://example.com./EWS/Exchange.asmx", required = true)
    private String ews_url;

    @JsonProperty("interval")
    @Schema(description = "Interval in minutes", example = "1", minimum = "1")
    private Long interval;

    @JsonProperty("username")
    @Schema(description = "Username for EWS", example = "username", required = true)
    private String username;

    @JsonProperty("password")
    @Schema(description = "Password for EWS", example = "password", required = true)
    private String password;
}

