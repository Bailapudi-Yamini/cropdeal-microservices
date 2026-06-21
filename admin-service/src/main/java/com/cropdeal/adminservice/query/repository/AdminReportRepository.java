package com.cropdeal.adminservice.query.repository;

import com.cropdeal.adminservice.query.model.AdminReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminReportRepository extends JpaRepository<AdminReport, Long> {
    Page<AdminReport> findByReportTypeOrderByGeneratedAtDesc(String reportType, Pageable pageable);
    Page<AdminReport> findByGeneratedByOrderByGeneratedAtDesc(Long generatedBy, Pageable pageable);
}
