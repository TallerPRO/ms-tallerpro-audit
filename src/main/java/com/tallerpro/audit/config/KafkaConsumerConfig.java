package com.tallerpro.audit.config;

import com.tallerpro.audit.dto.AuditEventMessage;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Consumer de jobs.events / audit.timeline. Mismos principios de
 * resiliencia que ms-tallerpro-report: concurrencia, ack por registro
 * y DLT PROPIO por topico de origen, sin ninguna llamada saliente hacia
 * ms-tallerpro-jobs u otro microservicio:
 *
 *  - jobs.events    -> jobs.events.audit.DLT (no se comparte con report:
 *                      reprocesar los fallos de audit no los reinyecta en report)
 *  - audit.timeline -> audit.timeline.DLT    (audit es su unico consumidor)
 *
 * El DLT se publica sin particion fija (Kafka elige por clave), asi no
 * depende de que tenga tantas particiones como el topico de origen.
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${app.kafka.topics.jobs-events}")
    private String jobsEvents;

    @Value("${app.kafka.topics.audit-timeline}")
    private String auditTimeline;

    @Value("${app.kafka.topics.jobs-events-dlt:jobs.events.audit.DLT}")
    private String jobsEventsDlt;

    @Value("${app.kafka.topics.audit-timeline-dlt:audit.timeline.DLT}")
    private String auditTimelineDlt;

    @Value("${app.kafka.dlt.partitions:3}")
    private int dltPartitions;

    @Value("${app.kafka.dlt.replicas:1}")
    private int dltReplicas;

    @Value("${app.kafka.dlt.retention-ms:2592000000}")
    private String dltRetentionMs;

    @Value("${app.kafka.audit-timeline.min-compaction-lag-ms:604800000}")
    private String auditTimelineMinCompactionLagMs;

    /**
     * Topicos que pertenecen a audit:
     *  - Sus DLT, con retencion larga (30 dias por defecto).
     *  - audit.timeline como "espejo compactado" (cleanup.policy=compact): conserva el ultimo
     *    evento por clave (orderId). min.compaction.lag.ms garantiza que ningun evento se compacte
     *    antes de 7 dias, para que audit alcance a persistir todos los intermedios en su BD.
     *    Ojo: un topico compactado rechaza mensajes sin clave.
     */
    @Bean
    public KafkaAdmin.NewTopics auditTopics() {
        NewTopic timeline = TopicBuilder.name(auditTimeline)
                .partitions(dltPartitions)
                .replicas(dltReplicas)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT)
                .config(TopicConfig.MIN_COMPACTION_LAG_MS_CONFIG, auditTimelineMinCompactionLagMs)
                .build();
        return new KafkaAdmin.NewTopics(timeline, dlt(jobsEventsDlt), dlt(auditTimelineDlt));
    }

    private NewTopic dlt(String nombre) {
        return TopicBuilder.name(nombre)
                .partitions(dltPartitions)
                .replicas(dltReplicas)
                .config(TopicConfig.RETENTION_MS_CONFIG, dltRetentionMs)
                .build();
    }

    /** Topico de origen -> DLT propio de audit. */
    String dltPara(String topicoOrigen) {
        if (jobsEvents.equals(topicoOrigen)) {
            return jobsEventsDlt;
        }
        if (auditTimeline.equals(topicoOrigen)) {
            return auditTimelineDlt;
        }
        return topicoOrigen + ".audit.DLT";
    }

    @Bean
    public ConsumerFactory<String, AuditEventMessage> consumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties(null));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.tallerpro.audit.dto");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, AuditEventMessage.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AuditEventMessage> kafkaListenerContainerFactory(
            ConsumerFactory<String, AuditEventMessage> consumerFactory,
            KafkaOperations<Object, Object> kafkaOperations) {

        ConcurrentKafkaListenerContainerFactory<String, AuditEventMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);
        // MANUAL_IMMEDIATE y no RECORD: el listener recibe un `Acknowledgment` y
        // confirma el offset el mismo. Con RECORD, Spring Kafka no inyecta ese
        // argumento y todo mensaje termina en el DLT ("No Acknowledgment available
        // as an argument"). Los tests no lo detectan: no levantan el contenedor.
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Particion -1: el productor elige la particion del DLT (ver javadoc de la clase).
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaOperations,
                (record, ex) -> new TopicPartition(dltPara(record.topic()), -1));
        factory.setCommonErrorHandler(new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L)));

        return factory;
    }
}
