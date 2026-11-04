package com.jobportal.dto;

import com.jobportal.entity.JobStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record JobRequest(
        @NotBlank(message = "Job title is required")
        @Size(max = 255, message = "Job title must not exceed 255 characters")
        String title,

        @NotBlank(message = "Job description is required")
        String description,

        @NotNull(message = "Minimum salary is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Minimum salary must be non-negative")
        BigDecimal salaryMin,

        @NotNull(message = "Maximum salary is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Maximum salary must be non-negative")
        BigDecimal salaryMax,

        @NotBlank(message = "Job location is required")
        @Size(max = 255, message = "Job location must not exceed 255 characters")
        String location,

        JobStatus status,

        @NotNull(message = "Company ID is required")
        Long companyId) {
}
