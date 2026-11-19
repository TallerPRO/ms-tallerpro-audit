package com.tallerpro.audit.config;

import com.tallerpro.audit.dto.AuditEventMessage;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El DLT recibe el valor ya deserializado (DTO) cuando falla el procesamiento, o los bytes
 * originales cuando falla la deserializacion. El template del DLT debe aceptar ambos.
 */
class DltSerializerTest {

    @SuppressWarnings({"unchecked", "rawtypes"})
    private org.apache.kafka.common.serialization.Serializer<Object> valueSerializer() {
        KafkaTemplate<Object, Object> template = new KafkaConsumerConfig().dltKafkaTemplate(new KafkaProperties());
        DefaultKafkaProducerFactory pf = (DefaultKafkaProducerFactory) template.getProducerFactory();
        return (org.apache.kafka.common.serialization.Serializer<Object>) pf.getValueSerializerSupplier().get();
    }

    @Test
    void serializaElDtoComoJson() {
        AuditEventMessage dto = TestData.dto();
        String json = new String(valueSerializer().serialize("t.DLT", dto), StandardCharsets.UTF_8);
        assertThat(json).contains("\"eventId\":\"ev-1\"").contains("\"orderId\":\"ORD-1\"");
    }

    @Test
    void conservaLosBytesOriginales() {
        byte[] crudo = "{json roto".getBytes(StandardCharsets.UTF_8);
        assertThat(valueSerializer().serialize("t.DLT", crudo)).isEqualTo(crudo);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void elStringSerializerPorDefectoFallabaConElDto() {
        org.apache.kafka.common.serialization.Serializer s = new StringSerializer();
        assertThatThrownBy(() -> s.serialize("t.DLT", TestData.dto())).isInstanceOf(ClassCastException.class);
    }
}
