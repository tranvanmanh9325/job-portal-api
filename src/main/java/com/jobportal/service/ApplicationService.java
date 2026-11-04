package com.jobportal.service;

import com.jobportal.dto.ApplicationRequest;
import com.jobportal.dto.ApplicationResponse;
import java.util.List;

public interface ApplicationService {

    ApplicationResponse applyForJob(Long jobId, ApplicationRequest request);

    List<ApplicationResponse> getApplicationsByJobId(Long jobId);
}
