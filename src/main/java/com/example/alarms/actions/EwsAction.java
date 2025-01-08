package com.example.alarms.actions;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.PropertySet;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.enumeration.search.SortDirection;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.core.service.item.Item;
import microsoft.exchange.webservices.data.core.service.schema.ItemSchema;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import microsoft.exchange.webservices.data.search.FindItemsResults;

import microsoft.exchange.webservices.data.search.ItemView;
import microsoft.exchange.webservices.data.search.filter.SearchFilter;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.Date;

@Setter
@Getter
public class EwsAction implements Action {
    private final String paramsJson;
    private final Long actionId;
    private Date lastChecked = null;

    @Getter
    private Params params;

    public EwsAction(String params, Long actionId) {
        this.paramsJson = params;
        this.actionId = actionId;
        mapParamsToFields();
    }

    private void mapParamsToFields() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.params = objectMapper.readValue(this.paramsJson, Params.class);
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
                ExchangeService service = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
                service.setCredentials(new WebCredentials(params.getUsername(), params.getPassword()));
                service.setUrl(new URI(params.getEws_url()));

                if (lastChecked == null) {
                    ItemView view = new ItemView(1);

                    view.getOrderBy().add(ItemSchema.DateTimeReceived, SortDirection.Descending);

                    // Fetch the latest email
                    FindItemsResults<Item> findResults = service.findItems(WellKnownFolderName.Inbox, view);

                    service.loadPropertiesForItems(findResults, PropertySet.FirstClassProperties);
                    // If we have results, update lastChecked with the timestamp of the most recent email
                    if (findResults.getTotalCount() != 0) {
                        Date latestEmailTime = findResults.getItems().getFirst().getDateTimeReceived();
                        lastChecked = latestEmailTime;
                        System.out.println("First call: Latest email timestamp is " + latestEmailTime);
                    }

                    return Flux.empty(); // No emails returned on the first call
                }

                ItemView view = new ItemView(100);
                view.getOrderBy().add(ItemSchema.DateTimeReceived, SortDirection.Descending);
                Date adjustedLastChecked = new Date(lastChecked.getTime() + 1000); // Add 1 millisecond
                SearchFilter filter = new SearchFilter.IsGreaterThan(ItemSchema.DateTimeReceived, new Date(0));
                FindItemsResults<Item> findResults = service.findItems(WellKnownFolderName.Inbox, filter, view);

                if (findResults.getTotalCount() != 0)
                    lastChecked = findResults.getItems().getFirst().getDateTimeReceived();

                // Return the email subject lines in a Flux
                return Flux.fromIterable(findResults)
                        .filter(item -> item instanceof EmailMessage)
                        .map(item -> {
                            try {
                                return EmailMessage.bind(service, item.getId());
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            return "";
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
}
