package com.jobportal.service.impl;

import com.jobportal.dto.JobRequest;
import com.jobportal.dto.JobResponse;
import com.jobportal.dto.PageResponse;
import com.jobportal.entity.Company;
import com.jobportal.entity.Job;
import com.jobportal.entity.JobStatus;
import com.jobportal.exception.BusinessRuleException;
import com.jobportal.exception.ResourceNotFoundException;
import com.jobportal.repository.JobRepository;
import com.jobportal.repository.JobSpecification;
import com.jobportal.service.CompanyService;
import com.jobportal.service.JobService;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final CompanyService companyService;

    public JobServiceImpl(JobRepository jobRepository, CompanyService companyService) {
        this.jobRepository = jobRepository;
        this.companyService = companyService;
    }

    @Override
    @Transactional
    public JobResponse createJob(JobRequest request) {
        validateSalaryRange(request.salaryMin(), request.salaryMax());
        Company company = companyService.findEntityById(request.companyId());

        Job job =
                new Job(
                        request.title().trim(),
                        request.description().trim(),
                        request.salaryMin(),
                        request.salaryMax(),
                        request.location().trim(),
                        request.status() != null ? request.status() : JobStatus.OPEN,
                        company);

        Job savedJob = jobRepository.save(job);
        return JobResponse.fromEntity(savedJob);
    }

    @Override
    public PageResponse<JobResponse> searchJobs(
            String keyword,
            String location,
            JobStatus status,
            Long companyId,
            Pageable pageable) {
        Specification<Job> spec =
                JobSpecification.withFilters(keyword, location, status, companyId);
        Page<JobResponse> pageResult =
                jobRepository.findAll(spec, pageable).map(JobResponse::fromEntity);
        return PageResponse.from(pageResult);
    }

    @Override
    public JobResponse getJobById(Long id) {
        return JobResponse.fromEntity(findEntityById(id));
    }

    @Override
    @Transactional
    public JobResponse updateJob(Long id, JobRequest request) {
        validateSalaryRange(request.salaryMin(), request.salaryMax());
        Job existingJob = findEntityById(id);
        Company company = companyService.findEntityById(request.companyId());

        existingJob.setTitle(request.title().trim());
        existingJob.setDescription(request.description().trim());
        existingJob.setSalaryMin(request.salaryMin());
        existingJob.setSalaryMax(request.salaryMax());
        existingJob.setLocation(request.location().trim());
        if (request.status() != null) {
            existingJob.setStatus(request.status());
        }
        existingJob.setCompany(company);

        Job updatedJob = jobRepository.save(existingJob);
        return JobResponse.fromEntity(updatedJob);
    }

    @Override
    @Transactional
    public void deleteJob(Long id) {
        Job existingJob = findEntityById(id);
        jobRepository.delete(existingJob);
    }

    @Override
    public Job findEntityById(Long id) {
        return jobRepository
                .findWithCompanyById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job", id));
    }

    private void validateSalaryRange(BigDecimal salaryMin, BigDecimal salaryMax) {
        if (salaryMin != null && salaryMax != null && salaryMin.compareTo(salaryMax) > 0) {
            throw new BusinessRuleException(
                    "Maximum salary must be greater than or equal to minimum salary");
        }
    }
}
