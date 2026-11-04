package com.jobportal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobportal.dto.CompanyRequest;
import com.jobportal.dto.CompanyResponse;
import com.jobportal.entity.Company;
import com.jobportal.exception.ResourceNotFoundException;
import com.jobportal.repository.CompanyRepository;
import com.jobportal.service.impl.CompanyServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyServiceImpl companyService;

    private Company sampleCompany;

    @BeforeEach
    void setUp() {
        sampleCompany = new Company("Viettel Group", "Cau Giay, Ha Noi", "https://viettel.com.vn");
        sampleCompany.setId(1L);
    }

    @Test
    @DisplayName("Should create company and normalize fields")
    void createCompany_WhenValidRequest_ShouldSaveAndReturnResponse() {
        CompanyRequest request =
                new CompanyRequest(
                        "  Viettel Group  ",
                        "  Cau Giay, Ha Noi ",
                        " https://viettel.com.vn ");

        when(companyRepository.save(any(Company.class)))
                .thenAnswer(
                        invocation -> {
                            Company saved = invocation.getArgument(0);
                            saved.setId(1L);
                            return saved;
                        });

        CompanyResponse response = companyService.createCompany(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Viettel Group");
        assertThat(response.address()).isEqualTo("Cau Giay, Ha Noi");
        assertThat(response.website()).isEqualTo("https://viettel.com.vn");
    }

    @Test
    @DisplayName("Should return company details when company exists")
    void getCompanyById_WhenExists_ShouldReturnResponse() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(sampleCompany));

        CompanyResponse response = companyService.getCompanyById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Viettel Group");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when company does not exist")
    void getCompanyById_WhenNotFound_ShouldThrowResourceNotFoundException() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.getCompanyById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Company with id 99 was not found");
    }

    @Test
    @DisplayName("Should update company fields when company exists")
    void updateCompany_WhenExists_ShouldUpdateAndReturnResponse() {
        CompanyRequest updateRequest =
                new CompanyRequest("Viettel Digital", "Nam Tu Liem, Ha Noi", "https://viettel.vn");

        when(companyRepository.findById(1L)).thenReturn(Optional.of(sampleCompany));
        when(companyRepository.save(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompanyResponse updated = companyService.updateCompany(1L, updateRequest);

        assertThat(updated.name()).isEqualTo("Viettel Digital");
        assertThat(updated.address()).isEqualTo("Nam Tu Liem, Ha Noi");
        assertThat(updated.website()).isEqualTo("https://viettel.vn");
    }

    @Test
    @DisplayName("Should delete company when company exists")
    void deleteCompany_WhenExists_ShouldInvokeRepositoryDelete() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(sampleCompany));

        companyService.deleteCompany(1L);

        verify(companyRepository).delete(sampleCompany);
    }
}
