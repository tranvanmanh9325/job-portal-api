package com.jobportal.dto;

import com.jobportal.entity.Application;
import java.time.OffsetDateTime;

public record ApplicationResponse(
        Long id,
        Long jobId,
        String jobTitle,
        String fullName,
        String email,
        String cvLink,
        OffsetDateTime appliedAt) {

    public static ApplicationResponse fromEntity(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getJob().getId(),
                application.getJob().getTitle(),
                application.getFullName(),
                application.getEmail(),
                application.getCvLink(),
                application.getAppliedAt());
    }
}
