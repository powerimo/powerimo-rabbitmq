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
        RabbitMessageHandler h = new StandardRabbitMessageHandler();
        h.addCommandHandler(MessageType.EVENT, "test1", this::handle1);
        Message message = Message.builder()
                .typeMessage(MessageType.EVENT)
                .name("test1")
                .build();
        h.handleMessage(message);
        Assertions.assertEquals(2, result1);
    }

    @Test
    void testInterceptor() {
        RabbitMessageHandler h = new StandardRabbitMessageHandler();
        result1 = -1;
        h.setInterceptor(this::handle1);
        Message message = Message.builder()
                .typeMessage(MessageType.EVENT)
                .name("test1")
                .build();
        h.handleMessage(message);
        Assertions.assertEquals(2, result1);
    }

    @Test
    void testUnknownMessage() {
        RabbitMessageHandler h = new StandardRabbitMessageHandler();
        result1 = -1;
        h.setUnsupportedCommandHandler(this::handle1);
        Message message = Message.builder()
                .typeMessage(MessageType.EVENT)
                .name("test1")
                .build();

        // event should be ignored and does have not handled by UnsupportedCommand handler
        h.handleMessage(message);
        Assertions.assertEquals(-1, result1);

        message.setTypeMessage(MessageType.TASK);
        h.handleMessage(message);
        Assertions.assertEquals(2, result1);

        // if unsupported handler is not set, handler has to raise an exception
        h.setUnsupportedCommandHandler(null);
        result1 = -1;
        Assertions.assertThrowsExactly(RabbitException.class, () -> h.handleMessage(message));

        // set standard handler
        h.setUnsupportedCommandHandler(new StandardUnsupportedMessageHandler());
        h.handleMessage(message);
    }

    private void handle1(Message m) {
        result1 = 2;
    }

}
