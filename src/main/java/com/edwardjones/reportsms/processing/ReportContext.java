package com.edwardjones.reportsms.processing;

import com.edwardjones.reportsms.domain.ReportType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ReportContext {
    private final String requestId;
    private final ReportType reportType;
    private final String requestedBy;
    private final String parametersJson;
    private final Instant requestedAt;
}

