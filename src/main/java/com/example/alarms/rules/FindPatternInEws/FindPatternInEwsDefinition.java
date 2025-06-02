package com.example.alarms.rules.FindPatternInEws;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * FindPatternInEws rule definition
 */
@Data
@Schema(description = "Definition for FindPatternInEws rule")
public class FindPatternInEwsDefinition {
    @Schema(
            description = "Patterns to search for",
            required = true
    )
    private List<PatternDefinition> patterns;

    @JsonProperty("interval")
    @Schema(description = "Interval in seconds", example = "60", minimum = "1", required = true)
    private Integer interval;

    @JsonProperty("repetition")
    @Schema(description = "Number of repetitions before triggering", example = "1", minimum = "1", required = true)
    private Integer repetition;

    @JsonProperty("alarm_message")
    @Schema(description = "Message to display when pattern is found", example = "pattern found", required = true)
    private String alarmMessage;

    @JsonProperty("start_time")
    @Schema(description = "Time in the day when to start looking for pattern", example = "13:00", required = true)
    private String startTime;

    @JsonProperty("end_time")
    @Schema(description = "Time in the day when to stop looking for pattern", example = "15:00", required = true)
    private String endTime;



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

