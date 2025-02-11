package com.example.alarms.actions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Flux;
import jakarta.mail.event.ConnectionEvent;
import jakarta.mail.*;
import jakarta.mail.event.ConnectionListener;
import jakarta.mail.event.MessageCountEvent;
import jakarta.mail.event.MessageCountListener;
import java.util.*;

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
    private Params params;

    @Getter
    private ExposedParams exposedParams;

    public GmailAction(String paramsJson, Long actionId) {
        this.paramsJson = paramsJson;
        this.actionId = actionId;
        mapParamsToFields();

        receivedMessages = new ArrayList<Message>();

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
                    System.out.println("Connection opened: " + e.getSource());
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
                    System.out.println("New messages arrived: " + messages.length);
                    receivedMessages.addAll(Arrays.asList(messages));
                }

                @Override
                public void messagesRemoved(MessageCountEvent event) {
                    System.out.println("Messages removed.");
                }
            });


        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private void mapParamsToFields() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.params = objectMapper.readValue(this.paramsJson, Params.class);
            this.exposedParams = objectMapper.readValue(this.paramsJson, ExposedParams.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse params: " + e.getMessage(), e);
        }
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

        return objectMapper.convertValue(exposedParams, new TypeReference<Map<String, Object>>() {});
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
