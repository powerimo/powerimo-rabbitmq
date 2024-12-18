import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.powerimo.rabbitmq.*;

class HandlerTests {
    boolean initialized = false;
    int result1 = 1;

    void init() {
        if (initialized)
            return;
    }

    @Test
    void test1() {
        MQMessageHandler h = new MQStandardMessageHandler();
        h.addCommandHandler(MQMessageType.EVENT, "test1", this::handle1);
        MQMessage message = MQMessage.builder()
                .typeMessage(MQMessageType.EVENT)
                .name("test1")
                .build();
        h.handleMessage(message);
        Assertions.assertEquals(2, result1);
    }

    @Test
    void testInterceptor() {
        MQMessageHandler h = new MQStandardMessageHandler();
        result1 = -1;
        h.setInterceptor(this::handle1);
        MQMessage message = MQMessage.builder()
                .typeMessage(MQMessageType.EVENT)
                .name("test1")
                .build();
        h.handleMessage(message);
        Assertions.assertEquals(2, result1);
    }

    @Test
    void testUnknownMessage() {
        MQMessageHandler h = new MQStandardMessageHandler();
        result1 = -1;
        h.setUnsupportedCommandHandler(this::handle1);
        MQMessage message = MQMessage.builder()
                .typeMessage(MQMessageType.EVENT)
                .name("test1")
                .build();

        // event should be ignored and does have not handled by UnsupportedCommand handler
        h.handleMessage(message);
        Assertions.assertEquals(-1, result1);

        message.setTypeMessage(MQMessageType.TASK);
        h.handleMessage(message);
        Assertions.assertEquals(2, result1);

        // if unsupported handler is not set, handler has to raise an exception
        h.setUnsupportedCommandHandler(null);
        result1 = -1;
        Assertions.assertThrowsExactly(PowerimoMqException.class, () -> h.handleMessage(message));

        // set standard handler
        h.setUnsupportedCommandHandler(new MQStandardUnsupportedMessageHandler());
        h.handleMessage(message);
    }

    private void handle1(MQMessage m) {
        result1 = 2;
    }

}
