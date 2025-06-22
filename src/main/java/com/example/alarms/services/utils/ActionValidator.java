package com.example.alarms.services.utils;

import com.example.alarms.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for validating action requests
 */
public class ActionValidator {

    /**
     * Validates an action creation request
     *
     * @param request The request to validate
     * @return List of validation errors, empty if valid
     */
    public static List<String> validateCreateUpdateRequest(Action request) {
        List<String> errors = new ArrayList<>();

        // Validate main request structure
        if (request == null) {
            errors.add("Request body cannot be null");
            return errors;
        }

        // Validate type
        if (request.getType() == null || request.getType().isEmpty()) {
            errors.add("Type is required");
        } else {
            Set<String> validTypes = new HashSet<>();
            validTypes.add("EwsAction");
            validTypes.add("GmailAction");

            if (!validTypes.contains(request.getType())) {
                errors.add("Type must be one of: " + String.join(", ", validTypes));
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> paramsMap = mapper.convertValue(request.getParams(),
                new TypeReference<Map<String, Object>>() {});

        // Validate params based on type
        if (request.getParams() == null) {
            errors.add("Params are required");
        } else {
            errors.addAll(validateParams(request.getType(), paramsMap));
        }

        // Validate rules
        if (request.getRules() == null) {
            errors.add("Rules are required");
        } else if (request.getRules().isEmpty()) {
            errors.add("Rules array cannot be empty");
        } else {
            int index = 0;
            for (Rule rule : request.getRules()) {  // Assuming request.getRules() returns a Set<Rule>
                errors.addAll(validateRule(rule, index));
                index++;
            }
        }

        return errors;
    }

    /**
     * Validates params based on action type
     */
    private static List<String> validateParams(String type, Map<String, Object> params) {
        List<String> errors = new ArrayList<>();

        if ("EwsAction".equals(type)) {
            // Required fields for EwsAction
            if (!params.containsKey("ews_url") || params.get("ews_url") == null ||
                    params.get("ews_url").toString().isEmpty()) {
                errors.add("EwsAction requires ews_url parameter");
            } else if (!isValidUrl(params.get("ews_url").toString())) {
                errors.add("ews_url must be a valid URL");
            }

            if (!params.containsKey("username") || params.get("username") == null ||
                    params.get("username").toString().isEmpty()) {
                errors.add("EwsAction requires username parameter");
            }

            if (!params.containsKey("password") || params.get("password") == null ||
                    params.get("password").toString().isEmpty()) {
                errors.add("EwsAction requires password parameter");
            }

            // Optional fields with validation
            if (params.containsKey("interval")) {
                Object intervalObj = params.get("interval");
                if (intervalObj instanceof Number) {
                    int interval = ((Number) intervalObj).intValue();
                    if (interval < 1) {
                        errors.add("interval must be at least 1");
                    }
                } else {
                    errors.add("interval must be a number");
                }
            }
        } else if ("GmailAction".equals(type)) {
            // Required fields for GmailAction
            if (!params.containsKey("client_id") || params.get("client_id") == null ||
                    params.get("client_id").toString().isEmpty()) {
                errors.add("GmailAction requires client_id parameter");
            }

            if (!params.containsKey("client_secret") || params.get("client_secret") == null ||
                    params.get("client_secret").toString().isEmpty()) {
                errors.add("GmailAction requires client_secret parameter");
            }

            if (!params.containsKey("refresh_token") || params.get("refresh_token") == null ||
                    params.get("refresh_token").toString().isEmpty()) {
                errors.add("GmailAction requires refresh_token parameter");
            }

            // Optional fields with validation
            if (params.containsKey("interval")) {
                Object intervalObj = params.get("interval");
                if (intervalObj instanceof Number) {
                    int interval = ((Number) intervalObj).intValue();
                    if (interval < 1) {
                        errors.add("interval must be at least 1");
                    }
                } else {
                    errors.add("interval must be a number");
                }
            }
        }

        return errors;
    }

    /**
     * Validates a rule
     */
    private static List<String> validateRule(Rule rule, int index) {
        List<String> errors = new ArrayList<>();
        String prefix = "Rule at index " + index + ": ";

        if (rule == null) {
            errors.add(prefix + "cannot be null");
            return errors;
        }

        // Validate name
        if (rule.getName() == null || rule.getName().isEmpty()) {
            errors.add(prefix + "name is required");
        } else {
            Set<String> validRuleNames = new HashSet<>();
            validRuleNames.add("FindPatternInEws");
            // Add other valid rule names here

            if (!validRuleNames.contains(rule.getName())) {
                errors.add(prefix + "name must be one of: " + String.join(", ", validRuleNames));
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> definitionMap = mapper.convertValue(rule.getDefinition(),
                new TypeReference<Map<String, Object>>() {});

        // Validate definition based on rule name
        if (rule.getDefinition() == null) {
            errors.add(prefix + "definition is required");
        } else if ("FindPatternInEws".equals(rule.getName())) {

            // Required fields for FindPatternInEws
            if (!definitionMap.containsKey("patterns") || definitionMap.get("patterns") == null) {
                errors.add(prefix + "definition requires pattern field");
            }

            if (!definitionMap.containsKey("interval")) {
                errors.add(prefix + "definition requires interval field");
            } else {
                Object intervalObj = definitionMap.get("interval");
                if (intervalObj instanceof Number) {
                    int interval = ((Number) intervalObj).intValue();
                    if (interval < 1) {
                        errors.add(prefix + "interval must be at least 1");
                    }
                } else {
                    errors.add(prefix + "interval must be a number");
                }
            }

            if (!definitionMap.containsKey("repetition")) {
                errors.add(prefix + "definition requires repetition field");
            } else {
                Object repObj = definitionMap.get("repetition");
                if (repObj instanceof Number) {
                    int repetition = ((Number) repObj).intValue();
                    if (repetition < 1) {
                        errors.add(prefix + "repetition must be at least 1");
                    }
                } else {
                    errors.add(prefix + "repetition must be a number");
                }
            }

            if (!definitionMap.containsKey("alarm_message") || definitionMap.get("alarm_message") == null ||
                    definitionMap.get("alarm_message").toString().isEmpty()) {
                errors.add(prefix + "definition requires alarm_message field");
            }
//
//            if (!definitionMap.containsKey("location") || definitionMap.get("location") == null ||
//                    definitionMap.get("location").toString().isEmpty()) {
//                errors.add(prefix + "definition requires location field");
//            } else {
//                Set<String> validLocations = new HashSet<>();
//                validLocations.add("body");
//                validLocations.add("subject");
//                validLocations.add("sender");
//
//                if (!validLocations.contains(definitionMap.get("location").toString())) {
//                    errors.add(prefix + "location must be one of: " + String.join(", ", validLocations));
//                }
//            }
        }

        // Validate reactions array (must be present, can be empty)
        if (rule.getReactions() == null) {
            errors.add(prefix + "reactions array is required (can be empty)");
        } else {

            int i = 0;
            for (Reaction reaction : rule.getReactions()) {  // Assuming request.getRules() returns a Set<Rule>
                errors.addAll(validateReaction(reaction, index, i));
                i++;
            }
        }

        return errors;
    }

    /**
     * Validates a reaction
     */
    private static List<String> validateReaction(Reaction reaction, int ruleIndex, int reactionIndex) {
        List<String> errors = new ArrayList<>();
        String prefix = "Rule at index " + ruleIndex + ", reaction at index " + reactionIndex + ": ";

        if (reaction == null) {
            errors.add(prefix + "cannot be null");
            return errors;
        }

        // Validate name
        if (reaction.getName() == null || reaction.getName().isEmpty()) {
            errors.add(prefix + "name is required");
        } else {
            Set<String> validReactionNames = new HashSet<>();
            validReactionNames.add("SendEmailReaction");
            // Add other valid reaction names here

            if (!validReactionNames.contains(reaction.getName())) {
                errors.add(prefix + "name must be one of: " + String.join(", ", validReactionNames));
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> paramsMap = mapper.convertValue(reaction.getParams(),
                new TypeReference<Map<String, Object>>() {});

        // Validate params based on reaction name
        if (reaction.getParams() == null) {
            errors.add(prefix + "params are required");
        } else if ("SendEmailReaction".equals(reaction.getName())) {

            if (!paramsMap.containsKey("email_address") || paramsMap.get("email_address") == null ||
                    paramsMap.get("email_address").toString().isEmpty()) {
                errors.add(prefix + "params requires email_address field");
            } else if (!isValidEmail(paramsMap.get("email_address").toString())) {
                errors.add(prefix + "email_address must be a valid email");
            }
        }

        return errors;
    }

    /**
     * Simple URL validation
     */
    private static boolean isValidUrl(String url) {
        try {
            new java.net.URL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Simple email validation
     */
    private static boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }
}