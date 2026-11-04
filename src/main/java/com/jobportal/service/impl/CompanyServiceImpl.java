package com.jobportal.service.impl;

import com.jobportal.dto.CompanyRequest;
import com.jobportal.dto.CompanyResponse;
import com.jobportal.entity.Company;
import com.jobportal.exception.ResourceNotFoundException;
import com.jobportal.repository.CompanyRepository;
import com.jobportal.service.CompanyService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyServiceImpl(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Override
    @Transactional
    public CompanyResponse createCompany(CompanyRequest request) {
        Company company =
                new Company(
                        request.name().trim(),
                        request.address().trim(),
                        normalizeOptionalString(request.website()));
        Company savedCompany = companyRepository.save(company);
        return CompanyResponse.fromEntity(savedCompany);
    }

    @Override
    public List<CompanyResponse> getAllCompanies() {
        return companyRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(CompanyResponse::fromEntity)
                .toList();
    }

    @Override
    public CompanyResponse getCompanyById(Long id) {
        return CompanyResponse.fromEntity(findEntityById(id));
    }

    @Override
    @Transactional
    public CompanyResponse updateCompany(Long id, CompanyRequest request) {
        Company company = findEntityById(id);
        company.setName(request.name().trim());
        company.setAddress(request.address().trim());
        company.setWebsite(normalizeOptionalString(request.website()));
        Company updatedCompany = companyRepository.save(company);
        return CompanyResponse.fromEntity(updatedCompany);
    }

    @Override
    @Transactional
    public void deleteCompany(Long id) {
        Company company = findEntityById(id);
        companyRepository.delete(company);
    }

    @Override
    public Company findEntityById(Long id) {
        return companyRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));
    }

    private String normalizeOptionalString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
