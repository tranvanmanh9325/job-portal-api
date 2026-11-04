package com.jobportal.service;

import com.jobportal.dto.JobRequest;
import com.jobportal.dto.JobResponse;
import com.jobportal.dto.PageResponse;
import com.jobportal.entity.Job;
import com.jobportal.entity.JobStatus;
import org.springframework.data.domain.Pageable;

public interface JobService {

    JobResponse createJob(JobRequest request);

    PageResponse<JobResponse> searchJobs(
            String keyword,
            String location,
            JobStatus status,
            Long companyId,
            Pageable pageable);

    JobResponse getJobById(Long id);

    JobResponse updateJob(Long id, JobRequest request);

    void deleteJob(Long id);

    Job findEntityById(Long id);
}
