package com.edulearn.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VideoPositionRequest {

    @NotNull(message = "Position is required")
    @Min(value = 0, message = "Position must be greater than or equal to 0")
    private Integer position;
}
