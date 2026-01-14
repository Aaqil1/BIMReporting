package com.edwardjones.reportsms.api.dto;

import com.edwardjones.reportsms.domain.ReportStatus;
import com.edwardjones.reportsms.domain.ReportType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ReportDetailsResponse {
    private final String requestId;
    private final ReportType reportType;
    private final ReportStatus status;
    private final String requestedBy;
    private final String parametersJson;
    private final String archiveRef;
    private final String errorMessage;
    private final Instant createdAt;
    private final Instant updatedAt;
}

