import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.powerimo.rabbitmq.*;

import java.time.OffsetDateTime;


class PowerMqTests {

    @Test
    void initContext() {

    }

    @Test
    void testStandardPayloadConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        StandardRabbitPayloadConverter converter = new StandardRabbitPayloadConverter(objectMapper);
        SamplePayload samplePayload = new SamplePayload();
        samplePayload.setIntValue(15);
        samplePayload.setStringValue("sample");
        samplePayload.setDateTimeValue(OffsetDateTime.now());
        Message message = Message.builder()
                .typeMessage(MessageType.EVENT)
                .name("sample event")
                .messageId("testId")
                .result("OK")
                .resultCode(0)
                .sourceMessageId("s1")
                .sourceSenderId("s2")
                .protocolVersion("test")
                .build();
        message.setPayload(samplePayload);
        SamplePayload convertedPayload = converter.extractPayload(message, SamplePayload.class);
        Assertions.assertEquals(SamplePayload.class.getName(), message.getPayloadClass());
        Assertions.assertEquals(samplePayload.getIntValue(), convertedPayload.getIntValue());
        Assertions.assertEquals(samplePayload.getStringValue(), convertedPayload.getStringValue());
        Assertions.assertEquals(samplePayload.getDateTimeValue(), convertedPayload.getDateTimeValue());

        var rawPayload = converter.serializePayload(samplePayload);
        message.setPayload(new String(rawPayload));
        Assertions.assertNotNull(converter.extractPayload(message, SamplePayload.class));
    }

    @Test
    void testStandardPayloadConverterExceptions() {
        ObjectMapper objectMapper = new ObjectMapper();
        StandardRabbitPayloadConverter payloadConverter = new StandardRabbitPayloadConverter(objectMapper);
        Message message = new Message();
        Assertions.assertNull(payloadConverter.extractPayload(message, SamplePayload.class));
        // set wrong payload class
        message.setPayload(123);
        Assertions.assertThrowsExactly(RabbitException.class, () -> payloadConverter.extractPayload(null, SamplePayload.class));
        Assertions.assertThrowsExactly(RabbitException.class, () -> payloadConverter.extractPayload(message, SamplePayload.class));
    }

    @Test
    void testUtils1() {
        Message message = Message.builder()
                .build();
        Assertions.assertEquals(MessageType.UNKNOWN, RabbitUtils.getTypeMessage(message));
        Assertions.assertEquals(MessageType.TASK_RESULT, RabbitUtils.getTypeMessage("tAsK_rEsult"));

        message.setTypeMessage(MessageType.EVENT);
    }

    @Test
    void testUtils2() {
        Assertions.assertEquals(10, RabbitUtils.stringToIntegerDef("asda", 10));
    }
}
