package com.example.alarms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {
    private Long id;
    private String status;
    private String lockedBy;
    private Action action;
    private LocalDateTime createdDate;
    private LocalDateTime lockedAt;
}
