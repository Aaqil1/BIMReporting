package com.edwardjones.reportsms.kafka.event;

import com.edwardjones.reportsms.domain.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequestedEvent {
    private String requestId;
    private ReportType reportType;
    private String requestedBy;
    private String parametersJson;
    private Instant requestedAt;
}

