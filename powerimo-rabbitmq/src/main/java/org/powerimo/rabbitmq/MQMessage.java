package org.powerimo.rabbitmq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MQMessage {
    // common headers
    private String protocolVersion = "1.0";
    private String messageId;
    private String senderId;
    private MQMessageType typeMessage;
    private String typeMessageOriginalString;
    private OffsetDateTime messageDate = OffsetDateTime.now(ZoneId.of(ZoneOffset.UTC.getId()));
    private String name;
    // for TYPE=taskResult
    private String sourceMessageId;
    private String sourceSenderId;
    private String result;
    private Integer resultCode;
    // params and payload
    private String payloadClass;
    private HashMap<String, Object> params = new HashMap<>();
    private Object payload;
    private String contentType;
    private String routingKey;
    private String processId;

    public Object getParam(String name) {
        return params.get(name);
    }

    public Integer getParamAsInteger(final String name) {
        Object v = params.get(name);
        if (v == null) {
            return null;
        }
        String s = v.toString();
        return MQUtils.stringToIntegerDef(s, null);
    }

    public String getParamAsString(final String name) {
        Object obj = params.get(name);
        if (obj == null)
            return null;
        return obj.toString();
    }

    /**
     * get UUID parameter value
     *
     * @param name the name of parameter
     * @return value if exists, else throw PowerimoRuntimeException
     */
    public UUID getParamAsUUID(final String name) {
        String s = getParamAsString(name);
        if (s == null)
            return null;
        try {
            return UUID.fromString(s);
        } catch (Throwable ex) {
            throw new PowerimoMqException("The parameter value is not UUID: " + s);
        }
    }

    public LocalDate getParamAsLocalDate(final String name) {
        String s = getParamAsString(name);
        if (s == null)
            return null;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            formatter = formatter.withZone(ZoneId.systemDefault());
            LocalDate date = LocalDate.parse(s, formatter);
            return date;
        } catch (Throwable ex) {
            throw new PowerimoMqException("The parameter value is not LocalDate (yyyy-MM-dd): " + s);
        }
    }

    public MQMessage addParam(String name, Object value) {
        params.put(name, value);
        return this;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
        if (payload == null) {
            payloadClass = null;
        } else {
            payloadClass = payload.getClass().getName();
        }
    }
}
