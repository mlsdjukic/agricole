package com.example.alarms.rules.FindPatternInEws;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data; /**
 * FindPatternInEws rule definition
 */
@Data
@Schema(description = "Definition for FindPatternInEws rule")
public class FindPatternInEwsDefinition {
    @JsonProperty("pattern")
    @Schema(description = "Pattern to search for", example = "maybe allowed", required = true)
    private String pattern;

    @JsonProperty("interval")
    @Schema(description = "Interval in seconds", example = "60", minimum = "1", required = true)
    private Integer interval;

    @JsonProperty("repetition")
    @Schema(description = "Number of repetitions before triggering", example = "1", minimum = "1", required = true)
    private Integer repetition;

    @JsonProperty("alarm_message")
    @Schema(description = "Message to display when pattern is found", example = "pattern found", required = true)
    private String alarmMessage;

    @JsonProperty("location")
    @Schema(description = "Where to search for the pattern",
            example = "body",
            allowableValues = {"body", "subject", "sender"},
            required = true)
    private String location;
}