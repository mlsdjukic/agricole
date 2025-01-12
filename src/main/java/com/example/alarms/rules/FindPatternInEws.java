package com.example.alarms.rules;


import com.example.alarms.dto.AlarmRequestDTO;
import com.example.alarms.services.AlarmService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import microsoft.exchange.webservices.data.core.exception.service.local.ServiceLocalException;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;

import java.util.ArrayList;
import java.util.List;



public class FindPatternInEws implements  Rule{

    private final Long ruleId;
    private final List<Long> patternTimestamps;
    private Params params;

    private final AlarmService alarmService;

    public FindPatternInEws(String rulesJson, Long ruleId, AlarmService alarmService) {
        this.ruleId = ruleId;
        this.alarmService = alarmService;
        this.patternTimestamps = new ArrayList<>();
        mapParamsToFields(rulesJson);
    }

    private void mapParamsToFields(String rulesJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.params = objectMapper.readValue(rulesJson, Params.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse params: " + e.getMessage(), e);
        }
    }

    @Override
    public void execute(Object data) {
        System.out.println("Executing rule with id: " + this.ruleId);

        if (data instanceof EmailMessage email){
            long currentTime = System.currentTimeMillis();
            try {
                String container;

                switch (this.params.getLocation()) {
                    case "body":
                        container = email.getBody().toString();
                        break;
                    case "subject":
                        container = email.getSubject();
                        break;
                    case "sender":
                        container = email.getSender().toString();
                        break;
                    default:
                        System.out.println("Unsupported location " + params.getLocation());
                        return;
                }

                if (container.contains(params.getPattern())) {
                    patternTimestamps.add(currentTime);

                    // Remove timestamps outside the interval
                    patternTimestamps.removeIf(timestamp -> currentTime - timestamp > params.getInterval());

                    // Notify if repetition count is reached within the interval
                    if (patternTimestamps.size() >= params.getRepetition()) {
                        notifyPatternFound();
                        // Reset the timestamps after notifying
                        patternTimestamps.clear();
                    }
                }
            } catch (ServiceLocalException e) {
                e.printStackTrace();
            }

        }

    }

    private void notifyPatternFound() {
        String message = "Pattern '" + params.getPattern() + "' found " + params.getRepetition() + " times within " + params.getInterval() + " ms!";
        System.out.println(message);
        alarmService.save(new AlarmRequestDTO(this.ruleId, message))
                .subscribe();
    }

    @Setter
    @Getter
    private static class Params {

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
