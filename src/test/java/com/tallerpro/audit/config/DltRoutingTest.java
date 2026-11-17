package com.tallerpro.audit.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/** Cada topico de origen va a un DLT propio de audit; jobs.events NO usa el DLT de report. */
class DltRoutingTest {

    private KafkaConsumerConfig config;

    @BeforeEach
    void setUp() {
        config = new KafkaConsumerConfig();
        ReflectionTestUtils.setField(config, "jobsEvents", "jobs.events");
        ReflectionTestUtils.setField(config, "auditTimeline", "audit.timeline");
        ReflectionTestUtils.setField(config, "jobsEventsDlt", "jobs.events.audit.DLT");
        ReflectionTestUtils.setField(config, "auditTimelineDlt", "audit.timeline.DLT");
    }

    @Test
    void jobsEventsVaAlDltPropioDeAudit() {
        assertThat(config.dltPara("jobs.events")).isEqualTo("jobs.events.audit.DLT");
    }

    @Test
    void auditTimelineVaASuDlt() {
        assertThat(config.dltPara("audit.timeline")).isEqualTo("audit.timeline.DLT");
    }

    @Test
    void topicoDesconocidoUsaSufijoDeAudit() {
        assertThat(config.dltPara("otro.topico")).isEqualTo("otro.topico.audit.DLT");
    }
}
