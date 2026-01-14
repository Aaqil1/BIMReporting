package com.edwardjones.reportsms.kafka;

import com.edwardjones.reportsms.domain.ReportRequest;
import com.edwardjones.reportsms.domain.ReportStatus;
import com.edwardjones.reportsms.kafka.event.ReportRequestedEvent;
import com.edwardjones.reportsms.processing.ReportContext;
import com.edwardjones.reportsms.processing.ReportGenerationResult;
import com.edwardjones.reportsms.processing.ReportStrategyFactory;
import com.edwardjones.reportsms.repository.ReportRequestRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReportRequestedListener {

    private static final Logger log = LoggerFactory.getLogger(ReportRequestedListener.class);

    private final ReportRequestRepository repository;
    private final ReportStrategyFactory strategyFactory;

    @KafkaListener(topics = "bim-report-requested", groupId = "bim-report-workers", containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(ReportRequestedEvent event) {
        MDC.put("correlationId", event.getRequestId());
        try {
            process(event);
        } finally {
            MDC.remove("correlationId");
        }
    }

    @Transactional
    public void process(ReportRequestedEvent event) {
        Optional<ReportRequest> existingOpt = repository.findById(event.getRequestId());
        if (existingOpt.isEmpty()) {
            log.warn("Dropping event for unknown requestId {}", event.getRequestId());
            return;
        }
        ReportRequest request = existingOpt.get();

        if (request.getStatus() == ReportStatus.COMPLETED) {
            log.info("Skipping already completed request {}", request.getRequestId());
            return;
        }
        if (request.getStatus() == ReportStatus.IN_PROGRESS) {
            log.info("Skipping in-progress duplicate {}", request.getRequestId());
            return;
        }

        request.setStatus(ReportStatus.IN_PROGRESS);
        repository.saveAndFlush(request);

        try {
            ReportContext ctx = ReportContext.builder()
                    .requestId(request.getRequestId())
                    .reportType(request.getReportType())
                    .requestedBy(request.getRequestedBy())
                    .parametersJson(request.getParametersJson())
                    .requestedAt(event.getRequestedAt())
                    .build();

            ReportGenerationResult result = strategyFactory.getStrategy(request.getReportType())
                    .generate(ctx);

            request.setArchiveRef(result.getArchiveRef());
            request.setStatus(ReportStatus.COMPLETED);
            repository.save(request);
            log.info("Report {} completed with archiveRef {}", request.getRequestId(), result.getArchiveRef());
        } catch (Exception ex) {
            log.error("Report {} failed: {}", request.getRequestId(), ex.getMessage(), ex);
            request.setStatus(ReportStatus.FAILED);
            request.setErrorMessage(ex.getMessage());
            repository.save(request);
            throw ex instanceof RuntimeException ? (RuntimeException) ex : new RuntimeException(ex);
        }
    }
}

