package com.example.alarms.actions.EwsAction;

import com.example.alarms.actions.Action;
import com.example.alarms.components.ApplicationContextProvider;
import com.example.alarms.services.EnvService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.notification.EventType;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import microsoft.exchange.webservices.data.notification.GetEventsResults;
import microsoft.exchange.webservices.data.notification.ItemEvent;
import microsoft.exchange.webservices.data.notification.PullSubscription;
import microsoft.exchange.webservices.data.property.complex.FolderId;

import reactor.core.publisher.Flux;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@Slf4j
@Setter
@Getter
public class EwsAction implements Action {
    private final String paramsJson;
    private final Long actionId;
    private Date lastChecked = null;

    @Getter
    private EwsActionParams params;

    @Getter
    private ExposedParams exposedParams;

    private ExchangeService service;
    private PullSubscription subscription;

    private EnvService envService;

    public EwsAction(String jsonParams, Long actionId) throws Exception {
        this.envService = ApplicationContextProvider.getApplicationContext().getBean(EnvService.class);

        this.paramsJson = jsonParams;
        this.actionId = actionId;
        this.params = mapParamsToFields();

        Optional<EnvService.EwsAccountDetails> ewsPassword = envService.findByUrlAndUsername(this.params.getEws_url(), this.params.getUsername());
        if (ewsPassword.isEmpty()){
            throw new RuntimeException("Account not present in env");
        }

        service = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
        service.setCredentials(new WebCredentials(this.params.getUsername(), ewsPassword.get().getPassword()));
        try {
            service.setUrl(new URI(this.params.getEws_url().trim()));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // Subscribe to Pull Notifications for Inbox
        this.subscription = service.subscribeToPullNotifications(
                List.of(new FolderId(WellKnownFolderName.Inbox)),
                5,  // Timeout in minutes
                "",
                EventType.NewMail
        );

        log.info("Pull subscription created successfully!");
    }

    private EwsActionParams mapParamsToFields() {
        EwsActionParams params;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            params = objectMapper.readValue(this.paramsJson, EwsActionParams.class);
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

        return objectMapper.convertValue(exposedParams, new TypeReference<>() {});
    }

    @Setter
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ExposedParams {
        @JsonProperty("username")
        private String username;
    }
}
