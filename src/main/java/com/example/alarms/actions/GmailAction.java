package com.example.alarms.actions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Flux;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;
import java.util.Date;
import java.util.Properties;

@Setter
@Getter
public class GmailAction implements Action {

    private static final String IMAP_HOST = "imap.gmail.com";
    private static final String IMAP_PORT = "993";

    private final String paramsJson;
    private final Long actionId;
    private Date lastChecked = null;

    @Getter
    private Params params;

    @Getter
    private ExposedParams exposedParams;

    public GmailAction(String params, Long actionId) {
        this.paramsJson = params;
        this.actionId = actionId;
        mapParamsToFields();
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
            System.out.println("Executing action with id: " + this.actionId);
            try {
                // Create and configure the ExchangeService
                // Initialize the Exchange Service

                Properties properties = new Properties();
                properties.put("mail.imap.host", IMAP_HOST);
                properties.put("mail.imap.port", IMAP_PORT);
                properties.put("mail.imap.ssl.enable", "true");
                properties.put("mail.imap.auth", "true");
                properties.put("mail.imap.ssl.protocols", "TLSv1.2");

                Session session = Session.getInstance(properties, null);
                Store store = session.getStore("imap");
                store.connect(IMAP_HOST, params.getUsername(), params.getPassword());

                Folder inbox = store.getFolder("INBOX");
                inbox.open(Folder.READ_ONLY);

                if (lastChecked == null) {
                    int totalMessages = inbox.getMessageCount();

                    Message message = inbox.getMessage(totalMessages);

                    lastChecked = message.getReceivedDate();
                    return Flux.empty(); // No emails returned on the first call
                }

                SearchTerm searchTerm = new ReceivedDateTerm(ComparisonTerm.GT, lastChecked);
                Message[] messages = inbox.search(searchTerm);


                // Return the email subject lines in a Flux
                return Flux.fromArray(messages)
                        .filter(message -> {
                            try {
                                Date receivedDate = message.getReceivedDate();
                                if (receivedDate.after(lastChecked)) {
                                    lastChecked = receivedDate;
                                    return true;
                                }
                                return false;
                            } catch (Exception e) {
                                e.printStackTrace();
                                return false;
                            }
                        })
                        .map(item -> item)
                        ;
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
    public String getExposedParamsJson() {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            return objectMapper.writeValueAsString(exposedParams);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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
