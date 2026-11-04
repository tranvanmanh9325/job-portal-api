package com.jobportal.dto;

import com.jobportal.entity.Company;
import java.time.OffsetDateTime;

public record CompanyResponse(
        Long id,
        String name,
        String address,
        String website,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static CompanyResponse fromEntity(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getAddress(),
                company.getWebsite(),
                company.getCreatedAt(),
                company.getUpdatedAt());
    }
}
