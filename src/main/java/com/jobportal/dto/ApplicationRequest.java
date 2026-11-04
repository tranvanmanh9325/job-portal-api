package com.jobportal.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ApplicationRequest(
        @JsonAlias({"applicantName", "name"})
        @NotBlank(message = "Applicant full name is required")
        @Size(max = 255, message = "Full name must not exceed 255 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @JsonAlias({"cvUrl", "cv"})
        @NotBlank(message = "CV link is required")
        @Size(max = 500, message = "CV link must not exceed 500 characters")
        @Pattern(
                regexp = "^https?://.+",
                message = "CV link must be a valid HTTP or HTTPS URL")
        String cvLink) {
}
