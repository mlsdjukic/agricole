package com.example.alarms.rules;


import com.example.alarms.dto.NotificationDTO;
import com.example.alarms.reactions.Reaction;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import microsoft.exchange.webservices.data.core.exception.service.local.ServiceLocalException;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FindPatternInEws implements  Rule{

    private static final String defaultLocation = "body";
    private static final String defaultAlarmMessage = "Pattern found";

    private final Long ruleId;
    private final List<Long> patternTimestamps;
    private Params params;

    private final List<Reaction> reactions;


    public FindPatternInEws(String rulesJson, Long ruleId, List<Reaction> reactions) {
        this.ruleId = ruleId;
        this.reactions = reactions;
        this.patternTimestamps = new ArrayList<>();
        mapParamsToFields(rulesJson);
    }

    private void mapParamsToFields(String rulesJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.params = objectMapper.readValue(rulesJson, Params.class);
            validateParams();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse params: " + e.getMessage(), e);
        }
    }

    private void validateParams() {
        if (this.params.alarmMessage == null) {
            this.params.alarmMessage = defaultAlarmMessage;
        }

        if(this.params.location == null) {
            this.params.location = defaultLocation;
        }
    }

    @Override
    public void execute(Object data) {
        log.info("Executing rule with id: {}", this.ruleId);

        if (!(data instanceof EmailMessage email)) {
            return;
        }

        try {
            String container = extractContainer(email, this.params.getLocation());
            if (container == null) {
                return;
            }

            processPatternMatching(container);

        } catch (ServiceLocalException e) {
            log.error("Error executing rule: {}", e.getMessage() );
        }
    }

    private String extractContainer(EmailMessage email, String location) throws ServiceLocalException {
        return switch (location) {
            case "body" -> email.getBody().toString();
            case "subject" -> email.getSubject();
            case "sender" -> email.getSender().toString();
            default -> {
                log.error("Unsupported location {}", location);
                yield null;
            }
        };
    }

    private void processPatternMatching(String container) {
        long currentTime = System.currentTimeMillis();

        if (!container.toLowerCase().contains(params.getPattern().toLowerCase())) {
            return;
        }

        patternTimestamps.add(currentTime);

        // Remove timestamps outside the interval
        patternTimestamps.removeIf(timestamp -> currentTime - timestamp > params.getInterval());

        // Notify if repetition count is reached within the interval
        if (patternTimestamps.size() >= params.getRepetition()) {
            Mono.fromRunnable(this::react).subscribeOn(Schedulers.boundedElastic()).subscribe();
            patternTimestamps.clear();
        }
    }

    private void react() {
        reactions.forEach(reaction -> reaction.execute(new NotificationDTO(this.ruleId, params.alarmMessage)));
    }

    @Setter
    @Getter
    private static class Params {

        @JsonProperty("alarm_message")
        private String alarmMessage;

        @JsonProperty("location")
        private String location;

        @JsonProperty("repetition")
        private int repetition;

        @JsonProperty("pattern")
        private String pattern;

        @JsonProperty("interval")
        private int interval;

    }
}
