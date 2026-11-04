package com.jobportal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobportal.dto.JobRequest;
import com.jobportal.dto.JobResponse;
import com.jobportal.dto.PageResponse;
import com.jobportal.entity.Company;
import com.jobportal.entity.Job;
import com.jobportal.entity.JobStatus;
import com.jobportal.exception.BusinessRuleException;
import com.jobportal.exception.ResourceNotFoundException;
import com.jobportal.repository.JobRepository;
import com.jobportal.service.impl.JobServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CompanyService companyService;

    @InjectMocks
    private JobServiceImpl jobService;

    private Company sampleCompany;
    private Job sampleJob;

    @BeforeEach
    void setUp() {
        sampleCompany = new Company("VNG Corporation", "District 7, HCMC", "https://vng.com.vn");
        sampleCompany.setId(1L);

        sampleJob =
                new Job(
                        "Senior Java Engineer",
                        "Build high-throughput microservices",
                        new BigDecimal("2000.00"),
                        new BigDecimal("3500.00"),
                        "Ho Chi Minh",
                        JobStatus.OPEN,
                        sampleCompany);
        sampleJob.setId(5L);
    }

    @Test
    @DisplayName("Should create job and default status to OPEN when status is null")
    void createJob_WhenValidRequest_ShouldSaveAndDefaultStatusToOpen() {
        JobRequest request =
                new JobRequest(
                        "Senior Java Engineer",
                        "Build high-throughput microservices",
                        new BigDecimal("2000.00"),
                        new BigDecimal("3500.00"),
                        "Ho Chi Minh",
                        null,
                        1L);

        when(companyService.findEntityById(1L)).thenReturn(sampleCompany);
        when(jobRepository.save(any(Job.class)))
                .thenAnswer(
                        invocation -> {
                            Job job = invocation.getArgument(0);
                            job.setId(5L);
                            return job;
                        });

        JobResponse response = jobService.createJob(request);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.title()).isEqualTo("Senior Java Engineer");
        assertThat(response.status()).isEqualTo(JobStatus.OPEN);
        assertThat(response.companyId()).isEqualTo(1L);
        assertThat(response.companyName()).isEqualTo("VNG Corporation");
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when salaryMin is greater than salaryMax")
    void createJob_WhenSalaryMinExceedsSalaryMax_ShouldThrowBusinessRuleException() {
        JobRequest invalidRequest =
                new JobRequest(
                        "Backend Engineer",
                        "Description",
                        new BigDecimal("4000.00"),
                        new BigDecimal("2000.00"),
                        "Ha Noi",
                        JobStatus.OPEN,
                        1L);

        assertThatThrownBy(() -> jobService.createJob(invalidRequest))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Maximum salary must be greater than or equal to minimum salary");

        verify(jobRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when creating job for non-existent company")
    void createJob_WhenCompanyNotFound_ShouldThrowResourceNotFoundException() {
        JobRequest request =
                new JobRequest(
                        "Backend Engineer",
                        "Description",
                        new BigDecimal("1000.00"),
                        new BigDecimal("2000.00"),
                        "Ha Noi",
                        JobStatus.OPEN,
                        99L);

        when(companyService.findEntityById(99L))
                .thenThrow(new ResourceNotFoundException("Company", 99L));

        assertThatThrownBy(() -> jobService.createJob(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Company with id 99 was not found");
    }

    @Test
    @DisplayName("Should search and return paginated jobs matching filters")
    @SuppressWarnings("unchecked")
    void searchJobs_WithKeywordAndLocationAndStatus_ShouldReturnPageResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        when(jobRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sampleJob), pageable, 1));

        PageResponse<JobResponse> pageResponse =
                jobService.searchJobs("Java", "Ho Chi Minh", JobStatus.OPEN, 1L, pageable);

        assertThat(pageResponse.totalElements()).isEqualTo(1);
        assertThat(pageResponse.content()).hasSize(1);
        assertThat(pageResponse.content().get(0).title()).isEqualTo("Senior Java Engineer");
    }

    @Test
    @DisplayName("Should update existing job status and fields")
    void updateJob_WhenJobExists_ShouldUpdateSuccessfully() {
        JobRequest updateRequest =
                new JobRequest(
                        "Lead Java Engineer",
                        "Updated description",
                        new BigDecimal("2500.00"),
                        new BigDecimal("4500.00"),
                        "Ho Chi Minh",
                        JobStatus.CLOSED,
                        1L);

        when(jobRepository.findWithCompanyById(5L)).thenReturn(Optional.of(sampleJob));
        when(companyService.findEntityById(1L)).thenReturn(sampleCompany);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobResponse updated = jobService.updateJob(5L, updateRequest);

        assertThat(updated.title()).isEqualTo("Lead Java Engineer");
        assertThat(updated.status()).isEqualTo(JobStatus.CLOSED);
        assertThat(updated.salaryMax()).isEqualByComparingTo("4500.00");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent job")
    void deleteJob_WhenJobDoesNotExist_ShouldThrowResourceNotFoundException() {
        when(jobRepository.findWithCompanyById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.deleteJob(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Job with id 404 was not found");
    }
}
