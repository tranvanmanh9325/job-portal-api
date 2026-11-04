package com.jobportal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobportal.dto.ApplicationRequest;
import com.jobportal.dto.CompanyRequest;
import com.jobportal.dto.JobRequest;
import com.jobportal.entity.JobStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JobPortalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Full workflow: Company CRUD, Job search/filter/pagination, Application rules and 400/404/409 JSON errors")
    void fullWorkflowAndErrorResponsesTest() throws Exception {
        // 1. Create Company (201 Created)
        CompanyRequest companyReq =
                new CompanyRequest(
                        "FPT Software",
                        "Cau Giay, Ha Noi",
                        "https://fptsoftware.com");
        MvcResult companyResult =
                mockMvc.perform(
                                post("/companies")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(companyReq)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.id").isNumber())
                        .andExpect(jsonPath("$.name").value("FPT Software"))
                        .andReturn();

        JsonNode companyJson =
                objectMapper.readTree(companyResult.getResponse().getContentAsString());
        Long companyId = companyJson.get("id").asLong();

        // 2. Create OPEN Job (201 Created)
        JobRequest openJobReq =
                new JobRequest(
                        "Spring Boot Backend Developer",
                        "Develop enterprise RESTful APIs with Java 17 and PostgreSQL",
                        new BigDecimal("1500.00"),
                        new BigDecimal("3000.00"),
                        "Ha Noi",
                        JobStatus.OPEN,
                        companyId);
        MvcResult openJobResult =
                mockMvc.perform(
                                post("/jobs")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(openJobReq)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value("OPEN"))
                        .andReturn();
        Long openJobId =
                objectMapper
                        .readTree(openJobResult.getResponse().getContentAsString())
                        .get("id")
                        .asLong();

        // 3. Create CLOSED Job (201 Created)
        JobRequest closedJobReq =
                new JobRequest(
                        "Frontend React Developer",
                        "Build modern UI components",
                        new BigDecimal("1200.00"),
                        new BigDecimal("2200.00"),
                        "Da Nang",
                        JobStatus.CLOSED,
                        companyId);
        MvcResult closedJobResult =
                mockMvc.perform(
                                post("/jobs")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(closedJobReq)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value("CLOSED"))
                        .andReturn();
        Long closedJobId =
                objectMapper
                        .readTree(closedJobResult.getResponse().getContentAsString())
                        .get("id")
                        .asLong();

        // 4. Test GET /jobs with keyword, location, status, pagination, and sorting
        mockMvc.perform(
                        get("/jobs")
                                .param("keyword", "Spring")
                                .param("location", "Ha Noi")
                                .param("status", "OPEN")
                                .param("page", "0")
                                .param("size", "5")
                                .param("sort", "salaryMax,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Spring Boot Backend Developer"))
                .andExpect(jsonPath("$.totalElements").value(1));

        // 5. Submit valid application to OPEN job (201 Created)
        ApplicationRequest appReq =
                new ApplicationRequest(
                        "Tran Van Manh",
                        "manh@example.com",
                        "https://example.com/cv/manh.pdf");
        mockMvc.perform(
                        post("/jobs/{id}/applications", openJobId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(appReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("manh@example.com"))
                .andExpect(jsonPath("$.appliedAt").isNotEmpty());

        // 6. Submit duplicate email to same job -> 409 Conflict with unified JSON
        ApplicationRequest duplicateAppReq =
                new ApplicationRequest(
                        "Tran Van Manh 2",
                        "MANH@example.com",
                        "https://example.com/cv/manh2.pdf");
        mockMvc.perform(
                        post("/jobs/{id}/applications", openJobId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(duplicateAppReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").exists());

        // 7. Submit application to CLOSED job -> 400 Bad Request with unified JSON
        mockMvc.perform(
                        post("/jobs/{id}/applications", closedJobId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(appReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));

        // 8. Validation error (invalid email & blank fullName) -> 400 Bad Request with fieldErrors
        ApplicationRequest invalidAppReq =
                new ApplicationRequest("", "not-an-email", "invalid-url");
        mockMvc.perform(
                        post("/jobs/{id}/applications", openJobId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidAppReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.fullName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.cvLink").exists());

        // 9. Resource Not Found -> 404 Not Found with unified JSON
        mockMvc.perform(get("/jobs/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));

        // 10. Update and Delete Company/Job
        JobRequest updateJobReq =
                new JobRequest(
                        "Senior Spring Boot Developer",
                        "Updated description",
                        new BigDecimal("2000.00"),
                        new BigDecimal("3500.00"),
                        "Ha Noi",
                        JobStatus.CLOSED,
                        companyId);
        mockMvc.perform(
                        put("/jobs/{id}", openJobId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateJobReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Senior Spring Boot Developer"))
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(delete("/jobs/{id}", closedJobId)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/companies/{id}", companyId)).andExpect(status().isNoContent());
    }
}
