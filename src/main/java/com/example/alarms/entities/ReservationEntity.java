package com.example.alarms.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table("reservations")
public class ReservationEntity {

    @Id
    private Long id;
    private String status;
    private String lockedBy;

    private Long actionId;

    @CreatedDate
    private LocalDateTime createdDate;

    private LocalDateTime locked_at;
}
