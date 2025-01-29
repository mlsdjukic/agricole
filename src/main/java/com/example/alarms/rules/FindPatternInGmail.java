package com.example.alarms.rules;


import com.example.alarms.dto.AlarmRequestDTO;
import com.example.alarms.dto.NotificationDTO;
import com.example.alarms.reactions.Reaction;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class FindPatternInGmail implements  Rule{

    private final Long ruleId;
    private final List<Long> patternTimestamps;
    private final List<Reaction> reactions;
    private Params params;

    public FindPatternInGmail(String rulesJson, Long ruleId, List<Reaction> reactions) {
        this.ruleId = ruleId;
        this.reactions = reactions;
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

        if (data instanceof Message email) {
            long currentTime = System.currentTimeMillis();
            try {
                StringBuilder container = new StringBuilder();

                switch (this.params.getLocation()) {
                    case "body":
                        Object content = email.getContent();
                        if (content instanceof String) {
                            container = new StringBuilder((String) content);
                        } else if (content instanceof Multipart multipart) {
                            for (int i = 0; i < multipart.getCount(); i++) {
                                BodyPart bodyPart = multipart.getBodyPart(i);
                                if (bodyPart.getContent() instanceof String) {
                                    container.append(bodyPart.getContent().toString());
                                }
                            }
                        }
                        break;
                    case "subject":

                        container = new StringBuilder(email.getSubject());
                        break;
                    case "sender":
                        container = new StringBuilder(Arrays.toString(email.getFrom()));
                        break;
                    default:
                        System.out.println("Unsupported location " + params.getLocation());
                        return;
                }

                if (container.toString().contains(params.getPattern())) {
                    System.out.println("pattern " + params.getPattern() + " found!");
                    patternTimestamps.add(currentTime);

                    // Remove timestamps outside the interval
                    patternTimestamps.removeIf(timestamp -> currentTime - timestamp > params.getInterval() * 1000L);

                    // Notify if repetition count is reached within the interval
                    if (patternTimestamps.size() >= params.getRepetition()) {

                        Mono.fromRunnable(this::react).subscribeOn(Schedulers.boundedElastic()).subscribe();
                        patternTimestamps.clear();
                    }
                }
            } catch (MessagingException | IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void react() {
        reactions.forEach(reaction -> {
            reaction.execute(new NotificationDTO(this.ruleId, params.alarmMessage));
        });
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
