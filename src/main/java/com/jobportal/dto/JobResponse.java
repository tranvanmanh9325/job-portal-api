package com.jobportal.dto;

import com.jobportal.entity.Job;
import com.jobportal.entity.JobStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record JobResponse(
        Long id,
        String title,
        String description,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String location,
        JobStatus status,
        Long companyId,
        String companyName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static JobResponse fromEntity(Job job) {
        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getLocation(),
                job.getStatus(),
                job.getCompany().getId(),
                job.getCompany().getName(),
                job.getCreatedAt(),
                job.getUpdatedAt());
    }
}
