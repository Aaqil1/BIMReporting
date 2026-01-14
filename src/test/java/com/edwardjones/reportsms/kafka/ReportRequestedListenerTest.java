package com.edwardjones.reportsms.kafka;

import com.edwardjones.reportsms.domain.ReportRequest;
import com.edwardjones.reportsms.domain.ReportStatus;
import com.edwardjones.reportsms.domain.ReportType;
import com.edwardjones.reportsms.kafka.event.ReportRequestedEvent;
import com.edwardjones.reportsms.processing.ReportGenerationResult;
import com.edwardjones.reportsms.processing.ReportStrategyFactory;
import com.edwardjones.reportsms.processing.strategy.ReportGenerationStrategy;
import com.edwardjones.reportsms.repository.ReportRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.*;

class ReportRequestedListenerTest {

    private ReportRequestedListener listener;
    private ReportRequestRepository repository;
    private ReportStrategyFactory factory;
    private ReportGenerationStrategy strategy;

    @BeforeEach
    void setup() {
        repository = mock(ReportRequestRepository.class);
        factory = mock(ReportStrategyFactory.class);
        strategy = mock(ReportGenerationStrategy.class);
        listener = new ReportRequestedListener(repository, factory);
    }

    @Test
    void skips_completed_requests() {
        ReportRequest req = ReportRequest.builder()
                .requestId("r1")
                .reportType(ReportType.PERFORMANCE)
                .status(ReportStatus.COMPLETED)
                .build();
        when(repository.findById("r1")).thenReturn(Optional.of(req));

        listener.process(event("r1"));

        verify(repository, never()).save(any());
    }

    @Test
    void processes_and_marks_completed() {
        ReportRequest req = ReportRequest.builder()
                .requestId("r2")
                .reportType(ReportType.PERFORMANCE)
                .status(ReportStatus.SUBMITTED)
                .build();
        when(repository.findById("r2")).thenReturn(Optional.of(req));
        when(factory.getStrategy(ReportType.PERFORMANCE)).thenReturn(strategy);
        when(strategy.generate(any())).thenReturn(new ReportGenerationResult("arch-1"));

        listener.process(event("r2"));

        verify(repository, times(1)).saveAndFlush(any());
        verify(repository, times(1)).save(any());
    }

    private ReportRequestedEvent event(String id) {
        return ReportRequestedEvent.builder()
                .requestId(id)
                .reportType(ReportType.PERFORMANCE)
                .requestedBy("user")
                .parametersJson("{}")
                .requestedAt(Instant.now())
                .build();
    }
}

