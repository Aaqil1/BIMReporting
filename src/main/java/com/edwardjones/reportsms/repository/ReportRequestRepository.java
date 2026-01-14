package com.edwardjones.reportsms.repository;

import com.edwardjones.reportsms.domain.ReportRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRequestRepository extends JpaRepository<ReportRequest, String> {
}

