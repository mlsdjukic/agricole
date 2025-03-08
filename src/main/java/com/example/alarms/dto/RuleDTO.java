package com.example.alarms.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class RuleDTO {
    private Long id;
    private String name;
    private Map<String,Object> definition;
    private List<ReactionDTO> reactions;
}
