package com.example.alarms.reactions;

import com.example.alarms.components.ApplicationContextProvider;
import com.example.alarms.dto.NotificationDTO;
import com.example.alarms.services.EmailService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.context.Context;

@Slf4j
public class SendEmailReaction implements Reaction{

    private Params parsedParams;

    private final EmailService emailService;
    private final Long ruleId;

    public SendEmailReaction(String params, String name, Long ruleId) {
        this.ruleId = ruleId;
        this.emailService = ApplicationContextProvider.getApplicationContext().getBean(EmailService.class);

        mapParamsToFields(params);
    }

    private void mapParamsToFields(String paramsJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.parsedParams = objectMapper.readValue(paramsJson, Params.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse params: " + e.getMessage(), e);
        }
    }

    @Override
    public Long getRuleId() {
        return this.ruleId;
    }

    @Override
    public void execute(NotificationDTO notification) {
            log.info("Send email to {} with message: {}", parsedParams.getEmailAddress(), notification.getMessage());

            Context context = new Context();

            context.setVariable("message", notification.getMessage());
            try {
                emailService.sendEmailWithHtmlTemplate(parsedParams.getEmailAddress(), "Rule satisfied", "email-template", context);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }


    }

    @Setter
    @Getter
    private static class Params {

        @JsonProperty("email_address")
        private String emailAddress;

    }
}
