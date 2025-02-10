package com.example.alarms.actions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.PropertySet;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.notification.EventType;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.enumeration.search.SortDirection;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.core.service.item.Item;
import microsoft.exchange.webservices.data.core.service.schema.ItemSchema;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import microsoft.exchange.webservices.data.notification.GetEventsResults;
import microsoft.exchange.webservices.data.notification.ItemEvent;
import microsoft.exchange.webservices.data.notification.NotificationEvent;
import microsoft.exchange.webservices.data.notification.PullSubscription;
import microsoft.exchange.webservices.data.property.complex.FolderId;
import microsoft.exchange.webservices.data.search.FindItemsResults;

import microsoft.exchange.webservices.data.search.ItemView;
import microsoft.exchange.webservices.data.search.filter.SearchFilter;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@Setter
@Getter
public class EwsAction implements Action {
    private final String paramsJson;
    private final Long actionId;
    private Date lastChecked = null;

    @Getter
    private Params params;

    @Getter
    private ExposedParams exposedParams;

    private ExchangeService service;
    private PullSubscription subscription;

    public EwsAction(String jsonParams, Long actionId) {
        this.paramsJson = jsonParams;
        this.actionId = actionId;
        this.params = mapParamsToFields();

        service = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
        service.setCredentials(new WebCredentials(this.params.getUsername(), this.params.getPassword()));
        try {
            service.setUrl(new URI(this.params.getEws_url().trim()));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // Subscribe to Pull Notifications for Inbox
        try {
            this.subscription = service.subscribeToPullNotifications(
                    List.of(new FolderId(WellKnownFolderName.Inbox)),
                    5,  // Timeout in minutes
                    "",
                    EventType.NewMail
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println("Pull subscription created successfully!");
    }

    private Params mapParamsToFields() {
        Params params;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            params = objectMapper.readValue(this.paramsJson, Params.class);
            this.exposedParams = objectMapper.readValue(this.paramsJson, ExposedParams.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse params: " + e.getMessage(), e);
        }

        return params;
    }

    @Override
    public Flux<Object> execute() {

        return Flux.create(sink -> {
            try {
                // Fetch new events
                GetEventsResults events = subscription.getEvents();

                for (ItemEvent event : events.getItemEvents()) {  // ✅ Correct method
                    if (event.getEventType() == EventType.NewMail) {
                        try {
                            // Fetch full email details
                            EmailMessage email = EmailMessage.bind(service, event.getItemId());
                            email.load();
                            sink.next(email);  // Emit email as an event
                        } catch (Exception e) {
                            sink.error(new RuntimeException("Failed to fetch email details", e));
                        }
                    }
                }
                sink.complete();  // Mark Flux as completed
            } catch (Exception e) {
                sink.error(new RuntimeException("Error fetching email events", e));
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

        @JsonProperty("ews_url")
        private String ews_url;

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
