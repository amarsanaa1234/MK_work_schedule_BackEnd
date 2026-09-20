package com.example.mk_backEnd.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NotificationResponse {

    private String id;
    private String message;
    private String jobAdId;
    private LocalDateTime createdAt;
    private boolean read;
}
