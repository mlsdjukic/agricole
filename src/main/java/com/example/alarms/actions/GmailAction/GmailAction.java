package com.example.alarms.actions.GmailAction;

import com.example.alarms.actions.Action;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import jakarta.mail.event.ConnectionEvent;
import jakarta.mail.*;
import jakarta.mail.event.ConnectionListener;
import jakarta.mail.event.MessageCountEvent;
import jakarta.mail.event.MessageCountListener;
import java.util.*;

@Slf4j
@Setter
@Getter
public class GmailAction implements Action {

    private static final String IMAP_HOST = "imap.gmail.com";
    private static final String IMAP_PORT = "993";

    private final String paramsJson;
    private final Long actionId;
    private Date lastChecked = null;

    private Store store;
    private Folder inbox;

    ArrayList<Message> receivedMessages;

    @Getter
    private GmailActionParams params;

    @Getter
    private ExposedParams exposedParams;

    public GmailAction(String paramsJson, Long actionId) {
        this.paramsJson = paramsJson;
        this.actionId = actionId;
        this.params = mapParamsToFields();

        receivedMessages = new ArrayList<>();

        Properties properties = new Properties();
        properties.put("mail.imap.host", IMAP_HOST);
        properties.put("mail.imap.port", IMAP_PORT);
        properties.put("mail.imap.ssl.enable", "true");
        properties.put("mail.imap.auth", "true");
        properties.put("mail.imap.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(properties, null);

        try {
            store = session.getStore("imap");
            store.addConnectionListener(new ConnectionListener() {
                @Override
                public void opened(ConnectionEvent e) {
                    log.info("Connection opened: {}", e.getSource());
                }

                @Override
                public void disconnected(ConnectionEvent e) {
                    try {
                        store.connect(IMAP_HOST, params.getUsername(), params.getPassword());
                        inbox = store.getFolder("INBOX");
                        inbox.open(Folder.READ_ONLY);
                    } catch (MessagingException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                @Override
                public void closed(ConnectionEvent e) {
                    try {
                        store.connect(IMAP_HOST, params.getUsername(), params.getPassword());
                        inbox = store.getFolder("INBOX");
                        inbox.open(Folder.READ_ONLY);
                    } catch (MessagingException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
            store.connect(IMAP_HOST, this.params.getUsername(), this.params.getPassword());

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            inbox.addMessageCountListener(new MessageCountListener() {
                @Override
                public void messagesAdded(MessageCountEvent event) {
                    Message[] messages = event.getMessages();
                    receivedMessages.addAll(Arrays.asList(messages));
                }

                @Override
                public void messagesRemoved(MessageCountEvent event) {
                    log.info("Messages removed.");
                }
            });


        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private GmailActionParams mapParamsToFields() {
        GmailActionParams params;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            params = objectMapper.readValue(this.paramsJson, GmailActionParams.class);
            this.exposedParams = objectMapper.readValue(this.paramsJson, ExposedParams.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse params: " + e.getMessage(), e);
        }
        return params;
    }

    @Override
    public Flux<Object> execute() {
        return Flux.defer(() -> {
            try {
                inbox.getMessageCount(); // Trigger server interaction to check for updates

                return Flux.defer(() -> {
                    Flux<Object> flux = Flux.fromIterable(new ArrayList<>(receivedMessages));
                    receivedMessages.clear();
                    return flux;
                });
            } catch (Exception e) {
                return Flux.empty();
            }
        });
    }

    @Override
    public Long getInterval(){
        return this.params.getInterval();
    }

    @Override
    public Map<String, Object> getExposedParamsJson() {
        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.convertValue(exposedParams, new TypeReference<>() {});
    }

    @Setter
    @Getter
    private static class Params {

        @JsonProperty("interval")
        private Long interval;

        @JsonProperty("username")
        private String username;

        @JsonProperty("password")
        private String password;

    }

    @Setter
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ExposedParams {
        @JsonProperty("username")
        private String username;
    }

}
