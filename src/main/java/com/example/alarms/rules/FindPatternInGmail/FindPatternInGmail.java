package com.example.alarms.rules.FindPatternInGmail;

import com.example.alarms.dto.Notification;
import com.example.alarms.reactions.Reaction;
import com.example.alarms.rules.Rule;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class FindPatternInGmail implements Rule {

    private final Long ruleId;
    private final List<Long> patternTimestamps;
    private final List<Reaction> reactions;
    private FindPatternInGmailDefinition params;

    public FindPatternInGmail(String rulesJson, Long ruleId, List<Reaction> reactions) {
        this.ruleId = ruleId;
        this.reactions = reactions;
        this.patternTimestamps = new ArrayList<>();
        mapParamsToFields(rulesJson);
    }

    private void mapParamsToFields(String rulesJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.params = objectMapper.readValue(rulesJson, FindPatternInGmailDefinition.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse params: " + e.getMessage(), e);
        }
    }

    @Override
    public void execute(Object data) {
        log.info("Executing rule with id: {}", this.ruleId);

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
                        log.error("Unsupported location {}", params.getLocation());
                        return;
                }

                if (container.toString().contains(params.getPattern())) {
                    log.info("pattern {} found!", params.getPattern());
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
                log.error("Error executing rule: {}", e.getMessage() );
            }

        }
    }

    private void react() {
        reactions.forEach(reaction -> reaction.execute(new Notification(this.ruleId, params.getAlarmMessage())));
    }
}
