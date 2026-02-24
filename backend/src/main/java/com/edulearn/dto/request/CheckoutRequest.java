package com.edulearn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CheckoutRequest {
    
    @NotEmpty(message = "Course IDs cannot be empty")
    private List<@NotBlank(message = "Course ID is required") String> courseIds;
    
    @NotBlank(message = "Payment method is required")
    private String paymentMethod;
}
