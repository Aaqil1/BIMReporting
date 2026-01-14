package com.edwardjones.reportsms.service;

import com.edwardjones.reportsms.api.dto.GenerateReportRequest;
import com.edwardjones.reportsms.api.dto.GenerateReportResponse;
import com.edwardjones.reportsms.api.dto.ReportDetailsResponse;
import com.edwardjones.reportsms.api.dto.ReportStatusResponse;
import com.edwardjones.reportsms.domain.ReportRequest;
import com.edwardjones.reportsms.domain.ReportStatus;
import com.edwardjones.reportsms.kafka.ReportEventPublisher;
import com.edwardjones.reportsms.kafka.event.ReportRequestedEvent;
import com.edwardjones.reportsms.repository.ReportRequestRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRequestRepository repository;
    private final ReportEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public GenerateReportResponse submitReport(GenerateReportRequest request) {
        String requestId = UUID.randomUUID().toString();
        MDC.put("correlationId", requestId);
        String paramsJson = toJson(request.getParameters());

        ReportRequest entity = ReportRequest.builder()
                .requestId(requestId)
                .reportType(request.getReportType())
                .status(ReportStatus.SUBMITTED)
                .requestedBy(request.getRequestedBy())
                .parametersJson(paramsJson)
                .build();

        repository.save(entity);

        ReportRequestedEvent event = ReportRequestedEvent.builder()
                .requestId(requestId)
                .reportType(request.getReportType())
                .requestedBy(request.getRequestedBy())
                .parametersJson(paramsJson)
                .requestedAt(Instant.now())
                .build();

        eventPublisher.publish(event);
        return new GenerateReportResponse(requestId);
    }

    @Transactional(readOnly = true)
    public ReportStatusResponse getStatus(String requestId) {
        ReportRequest req = repository.findById(requestId)
                .orElseThrow(() -> new ReportNotFoundException("Report not found: " + requestId));
        return new ReportStatusResponse(req.getRequestId(), req.getStatus(), req.getErrorMessage());
    }

    @Transactional(readOnly = true)
    public ReportDetailsResponse getDetails(String requestId) {
        ReportRequest req = repository.findById(requestId)
                .orElseThrow(() -> new ReportNotFoundException("Report not found: " + requestId));

        return ReportDetailsResponse.builder()
                .requestId(req.getRequestId())
                .reportType(req.getReportType())
                .status(req.getStatus())
                .requestedBy(req.getRequestedBy())
                .parametersJson(req.getParametersJson())
                .archiveRef(req.getArchiveRef())
                .errorMessage(req.getErrorMessage())
                .createdAt(req.getCreatedAt())
                .updatedAt(req.getUpdatedAt())
                .build();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid parameters", e);
        }
    }
}

