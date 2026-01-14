package com.edwardjones.reportsms.api.dto;

import com.edwardjones.reportsms.domain.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReportStatusResponse {
    private final String requestId;
    private final ReportStatus status;
    private final String errorMessage;
}

