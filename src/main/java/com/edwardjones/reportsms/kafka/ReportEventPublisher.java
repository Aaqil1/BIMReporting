package com.edwardjones.reportsms.kafka;

import com.edwardjones.reportsms.kafka.event.ReportRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReportEventPublisher.class);
    private static final String TOPIC = "bim-report-requested";

    private final KafkaTemplate<String, ReportRequestedEvent> kafkaTemplate;

    public void publish(ReportRequestedEvent event) {
        kafkaTemplate.send(TOPIC, event.getRequestId(), event);
        log.info("Published report request {} to topic {}", event.getRequestId(), TOPIC);
    }
}

