package com.edwardjones.reportsms.processing.strategy;

import com.edwardjones.reportsms.client.ArchiveDbClient;
import com.edwardjones.reportsms.processing.ReportContext;
import com.edwardjones.reportsms.processing.ReportGenerationResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PerformanceReportStrategy implements ReportGenerationStrategy {

    private static final Logger log = LoggerFactory.getLogger(PerformanceReportStrategy.class);

    private final ArchiveDbClient archiveDbClient;
    private final MeterRegistry meterRegistry;

    @Override
    public ReportGenerationResult generate(ReportContext context) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            String payload = """
                    {"reportType":"PERFORMANCE","requestId":"%s","parameters":%s}
                    """.formatted(context.getRequestId(), context.getParametersJson());
            String archiveRef = archiveDbClient.archiveReport(context.getRequestId(), payload);
            return new ReportGenerationResult(archiveRef);
        } finally {
            sample.stop(meterRegistry.timer("reports.generation.time", "reportType", "PERFORMANCE"));
            log.info("Performance report generated for {}", context.getRequestId());
        }
    }
}

