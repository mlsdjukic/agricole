package com.example.alarms.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("alarm_types")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AlarmTypeEntity {
    @Id
    private Long id;

    private String name;

    // Store metadata as raw JSON string (use @Column if needed)
    private String metadata;
}
