package com.jobportal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobportal.dto.ApplicationRequest;
import com.jobportal.dto.ApplicationResponse;
import com.jobportal.entity.Application;
import com.jobportal.entity.Company;
import com.jobportal.entity.Job;
import com.jobportal.entity.JobStatus;
import com.jobportal.exception.BusinessRuleException;
import com.jobportal.exception.DuplicateResourceException;
import com.jobportal.exception.ResourceNotFoundException;
import com.jobportal.repository.ApplicationRepository;
import com.jobportal.service.impl.ApplicationServiceImpl;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobService jobService;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    private Company sampleCompany;
    private Job openJob;
    private Job closedJob;

    @BeforeEach
    void setUp() {
        sampleCompany = new Company("FPT Software", "Cau Giay, Ha Noi", "https://fptsoftware.com");
        sampleCompany.setId(1L);

        openJob =
                new Job(
                        "Java Backend Developer",
                        "Spring Boot, PostgreSQL",
                        new BigDecimal("1500.00"),
                        new BigDecimal("2500.00"),
                        "Ha Noi",
                        JobStatus.OPEN,
                        sampleCompany);
        openJob.setId(10L);

        closedJob =
                new Job(
                        "Legacy Developer",
                        "Closed position",
                        new BigDecimal("1000.00"),
                        new BigDecimal("1500.00"),
                        "Da Nang",
                        JobStatus.CLOSED,
                        sampleCompany);
        closedJob.setId(20L);
    }

    @Test
    @DisplayName("Should create application successfully when job is OPEN and email is unique")
    void applyForJob_WhenJobIsOpenAndEmailIsUnique_ShouldSucceed() {
        ApplicationRequest request =
                new ApplicationRequest(
                        "Nguyen Van A",
                        "  NguyenVanA@Example.com ",
                        "https://cv.example.com/nguyenvana.pdf");

        when(jobService.findEntityById(10L)).thenReturn(openJob);
        when(applicationRepository.existsByJobIdAndEmailIgnoreCase(
                        10L, "nguyenvana@example.com"))
                .thenReturn(false);
        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(
                        invocation -> {
                            Application saved = invocation.getArgument(0);
                            saved.setId(100L);
                            saved.setAppliedAt(OffsetDateTime.now());
                            return saved;
                        });

        ApplicationResponse response = applicationService.applyForJob(10L, request);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.jobId()).isEqualTo(10L);
        assertThat(response.fullName()).isEqualTo("Nguyen Van A");
        assertThat(response.email()).isEqualTo("nguyenvana@example.com");
        assertThat(response.cvLink()).isEqualTo("https://cv.example.com/nguyenvana.pdf");
        assertThat(response.appliedAt()).isNotNull();
        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    @DisplayName("Should reject application when job status is CLOSED")
    void applyForJob_WhenJobIsClosed_ShouldThrowBusinessRuleException() {
        ApplicationRequest request =
                new ApplicationRequest(
                        "Tran Thi B",
                        "tranthib@example.com",
                        "https://cv.example.com/tranthib.pdf");

        when(jobService.findEntityById(20L)).thenReturn(closedJob);

        assertThatThrownBy(() -> applicationService.applyForJob(20L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CLOSED");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject application with 409 Conflict when email already applied to same job")
    void applyForJob_WhenDuplicateEmailInSameJob_ShouldThrowDuplicateResourceException() {
        ApplicationRequest request =
                new ApplicationRequest(
                        "Nguyen Van A",
                        "nguyenvana@example.com",
                        "https://cv.example.com/nguyenvana.pdf");

        when(jobService.findEntityById(10L)).thenReturn(openJob);
        when(applicationRepository.existsByJobIdAndEmailIgnoreCase(
                        10L, "nguyenvana@example.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> applicationService.applyForJob(10L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already applied");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw 404 ResourceNotFoundException when applying to non-existent job")
    void applyForJob_WhenJobDoesNotExist_ShouldThrowResourceNotFoundException() {
        ApplicationRequest request =
                new ApplicationRequest(
                        "Le Van C",
                        "levanc@example.com",
                        "https://cv.example.com/levanc.pdf");

        when(jobService.findEntityById(999L))
                .thenThrow(new ResourceNotFoundException("Job", 999L));

        assertThatThrownBy(() -> applicationService.applyForJob(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return list of applications for an existing job")
    void getApplicationsByJobId_WhenJobExists_ShouldReturnList() {
        Application app =
                new Application(
                        openJob,
                        "Nguyen Van A",
                        "nguyenvana@example.com",
                        "https://cv.example.com/a.pdf");
        app.setId(101L);
        app.setAppliedAt(OffsetDateTime.now());

        when(jobService.findEntityById(10L)).thenReturn(openJob);
        when(applicationRepository.findByJobIdOrderByAppliedAtDesc(10L)).thenReturn(List.of(app));

        List<ApplicationResponse> result = applicationService.getApplicationsByJobId(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("nguyenvana@example.com");
    }
}
