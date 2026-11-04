package com.jobportal.repository;

import com.jobportal.entity.Application;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByJobIdAndEmailIgnoreCase(Long jobId, String email);

    @EntityGraph(attributePaths = "job")
    List<Application> findByJobIdOrderByAppliedAtDesc(Long jobId);
}
