package com.example.alarms.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "alarm_class")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AlarmClassEntity {
    @Id
    private Long id;

    private String name;
    private String metadata;
}
