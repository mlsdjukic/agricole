package com.example.alarms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data; /**
 * Error response model
 */
@Data
@Schema(description = "Error response")
public class ErrorResponse {
    @Schema(description = "Error type", example = "Invalid request parameters")
    private String error;

    @Schema(description = "Detailed error message", example = "Rules array cannot be empty")
    private String message;

    @Schema(description = "HTTP status code", example = "400")
    private Integer status;
}
