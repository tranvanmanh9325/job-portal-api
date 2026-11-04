package com.jobportal.service.impl;

import com.jobportal.dto.ApplicationRequest;
import com.jobportal.dto.ApplicationResponse;
import com.jobportal.entity.Application;
import com.jobportal.entity.Job;
import com.jobportal.exception.BusinessRuleException;
import com.jobportal.exception.DuplicateResourceException;
import com.jobportal.repository.ApplicationRepository;
import com.jobportal.service.ApplicationService;
import com.jobportal.service.JobService;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobService jobService;

    public ApplicationServiceImpl(
            ApplicationRepository applicationRepository, JobService jobService) {
        this.applicationRepository = applicationRepository;
        this.jobService = jobService;
    }

    @Override
    @Transactional
    public ApplicationResponse applyForJob(Long jobId, ApplicationRequest request) {
        Job job = jobService.findEntityById(jobId);

        if (!job.isOpen()) {
            throw new BusinessRuleException(
                    String.format(
                            "Cannot submit application because job with id %d is CLOSED", jobId));
        }

        // Normalize email to lowercase to enforce case-insensitive uniqueness at both application and DB levels
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        if (applicationRepository.existsByJobIdAndEmailIgnoreCase(jobId, normalizedEmail)) {
            throw new DuplicateResourceException(
                    String.format(
                            "Email '%s' has already applied for job with id %d",
                            normalizedEmail, jobId));
        }

        Application application =
                new Application(
                        job,
                        request.fullName().trim(),
                        normalizedEmail,
                        request.cvLink().trim());

        Application savedApplication = applicationRepository.save(application);
        return ApplicationResponse.fromEntity(savedApplication);
    }

    @Override
    public List<ApplicationResponse> getApplicationsByJobId(Long jobId) {
        // Ensure the parent job exists before querying its applications
        jobService.findEntityById(jobId);
        return applicationRepository.findByJobIdOrderByAppliedAtDesc(jobId).stream()
                .map(ApplicationResponse::fromEntity)
                .toList();
    }
}
