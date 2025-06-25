package com.example.alarms.rules.DetectMissingEmail;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Definition for DetectMissingEmail rule")
public class DetectMissingEmailDefinition {
    @Schema(
            description = "Patterns to search for",
            required = true
    )
    private List<DetectMissingEmailDefinition.PatternDefinition> patterns;

    @JsonProperty("point_in_time")
    @Schema(description = "Point in time", example = "15:00", required = true)
    private String pointInTime;

    @JsonProperty("tolerance")
    @Schema(description = "Plus/minus around point_in_time in seconds", example = "5", required = false)
    private Integer tolerance;

    @JsonProperty("alarm_message")
    @Schema(description = "Message to display when pattern is found", example = "pattern found", required = true)
    private String alarmMessage;

    @Data
    @Schema(description = "Pattern definition")
    public static class PatternDefinition {

        @JsonProperty("pattern")
        @Schema(description = "The actual pattern string", example = "find me this", required = true)
        private String pattern;

        @JsonProperty("location")
        @Schema(description = "Where to search for the pattern",
                example = "body",
                allowableValues = {"body", "subject", "sender"},
                required = true)
        private String location;
    }
}



