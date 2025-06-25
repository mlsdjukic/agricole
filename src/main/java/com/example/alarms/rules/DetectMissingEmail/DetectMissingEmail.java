package com.example.alarms.rules.DetectMissingEmail;

import com.example.alarms.dto.Notification;
import com.example.alarms.reactions.Reaction;
import com.example.alarms.rules.DetectMissingEmail.DetectMissingEmailDefinition;
import com.example.alarms.rules.FindPatternInEws.FindPatternInEwsDefinition;
import com.example.alarms.rules.Rule;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import microsoft.exchange.webservices.data.core.exception.service.local.ServiceLocalException;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
public class DetectMissingEmail implements Rule {

    private final Long ruleId;
    private final List<Reaction> reactions;
    private DetectMissingEmailDefinition params;
    private Boolean emailReceived;
    private Boolean reacted;
    private LocalDate lastCheckDate;

    private Long alarmTypeId;
    private Long alarmClassId;

    private boolean firstRun;

    public DetectMissingEmail(String rulesJson, Long ruleId, List<Reaction> reactions, Long alarmTypeId, Long alarmClassId) {
        this.ruleId = ruleId;
        this.reactions = reactions;

        mapParamsToFields(rulesJson);
        this.emailReceived = false;
        this.reacted = false;
        this.lastCheckDate = null;

        this.alarmClassId = alarmClassId;
        this.alarmTypeId = alarmTypeId;

        this.firstRun = true;
    }

    private void mapParamsToFields(String rulesJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.params = objectMapper.readValue(rulesJson, DetectMissingEmailDefinition.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse params: " + e.getMessage(), e);
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

    @Override
    public void execute(Object data) {

        log.debug("Executing rule with id: {}", this.ruleId);

        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();

        if (lastCheckDate == null || !lastCheckDate.equals(today)) {
            this.reacted = false;
            lastCheckDate = today;
            log.debug("New day detected. Resetting hasMissedWindow flag.");
        }

        LocalTime windowStart = LocalTime.parse(this.params.getPointInTime()).minus(Duration.ofSeconds(this.params.getTolerance()));
        LocalTime windowEnd = LocalTime.parse(this.params.getPointInTime()).plus(Duration.ofSeconds(this.params.getTolerance()));

        if (now.isAfter(windowEnd) && this.firstRun){
            this.reacted = true; //on the first run time window is already passed so we simulate the reaction and wait for another day
        }

        this.firstRun = false;

        if (now.isBefore(windowStart) || this.reacted) {
            return;
        }

        if (now.isAfter(windowEnd) && !this.emailReceived) {
            Mono.fromRunnable(this::react).subscribeOn(Schedulers.boundedElastic()).subscribe();
            this.reacted = true;
            return;
        }

        if (!(data instanceof EmailMessage email)) {
            return;
        }

        try {
            LocalTime receivedTime = email.getDateTimeReceived()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime();

            for (DetectMissingEmailDefinition.PatternDefinition patternDefinition : this.params.getPatterns()) {
                String container = extractContainer(email, patternDefinition.getLocation());
                if (!receivedTime.isBefore(windowStart) && !receivedTime.isAfter(windowEnd)) {
                    if (container != null) {
                        if (container.toLowerCase().contains(patternDefinition.getPattern().toLowerCase())) {
                            this.emailReceived = true;
                            return;
                        }
                    }
                }
                this.emailReceived = false;
            }
        } catch (ServiceLocalException e) {
            log.error("Error executing rule: {}", e.getMessage() );
        }



    }


    private void react() {
        Notification notification = new Notification();
        notification.setRuleId(this.ruleId);
        notification.setMessage(params.getAlarmMessage());
        notification.setAlarmClassId(this.alarmClassId);
        notification.setAlarmTypeId(this.alarmTypeId);


        reactions.forEach(reaction -> reaction.execute(notification));
    }

    private boolean isWithinTolerance(Date actual, Date expected) {
        return Math.abs(actual.getTime() - expected.getTime()) <= this.params.getTolerance() * 60;
    }
}
