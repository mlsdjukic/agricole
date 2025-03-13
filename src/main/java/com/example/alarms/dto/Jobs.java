package com.example.alarms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Jobs {
    private Long actionId;
    private String type;
    private Map<String,Object> params;
    private List<Rule> rules;

}
