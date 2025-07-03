package com.example.alarms.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table("alarms")
public class AlarmEntity {

    @Id
    private Long id;
    private Long ruleId;
    private String message;
    private String status;
    private Boolean archived;
    private String createdFrom;
    private String metadata;
    private String relation;

    @Column("type_id")
    private Long alarmTypeId;

    @Column("class_id")
    private Long alarmClassId;

    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime updatedAt;


}
