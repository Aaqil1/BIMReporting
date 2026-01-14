package com.edwardjones.reportsms.service;

import com.edwardjones.reportsms.api.dto.GenerateReportRequest;
import com.edwardjones.reportsms.api.dto.GenerateReportResponse;
import com.edwardjones.reportsms.domain.ReportStatus;
import com.edwardjones.reportsms.domain.ReportType;
import com.edwardjones.reportsms.kafka.ReportEventPublisher;
import com.edwardjones.reportsms.repository.ReportRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReportServiceTest {

    private ReportService service;
    private ReportRequestRepository repository;
    private ReportEventPublisher publisher;
    private ObjectMapper mapper;

    @BeforeEach
    void setup() {
        repository = mock(ReportRequestRepository.class);
        publisher = mock(ReportEventPublisher.class);
        mapper = new ObjectMapper();
        service = new ReportService(repository, publisher, mapper);
    }

    @Test
    void submitReport_persists_and_publishes() {
        GenerateReportRequest request = new GenerateReportRequest();
        request.setReportType(ReportType.PERFORMANCE);
        request.setRequestedBy("user1");
        request.setParameters(Map.of("accountId", "A1"));

        GenerateReportResponse response = service.submitReport(request);

        assertThat(response.getRequestId()).isNotBlank();
        verify(repository, times(1)).save(any());
        verify(publisher, times(1)).publish(any());
    }

    @Test
    void getStatus_returns_status() {
        var entity = Instancio.of(com.edwardjones.reportsms.domain.ReportRequest.class)
                .set(Select.field("status"), ReportStatus.IN_PROGRESS)
                .create();
        when(repository.findById(entity.getRequestId())).thenReturn(java.util.Optional.of(entity));

        var status = service.getStatus(entity.getRequestId());

        assertThat(status.getStatus()).isEqualTo(ReportStatus.IN_PROGRESS);
    }
}

