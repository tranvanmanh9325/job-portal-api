package com.jobportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompanyRequest(
        @NotBlank(message = "Company name is required")
        @Size(max = 255, message = "Company name must not exceed 255 characters")
        String name,

        @NotBlank(message = "Company address is required")
        @Size(max = 500, message = "Company address must not exceed 500 characters")
        String address,

        @Size(max = 255, message = "Website URL must not exceed 255 characters")
        @Pattern(
                regexp = "^(https?://.+)?$",
                message = "Website must start with http:// or https://")
        String website) {
}
