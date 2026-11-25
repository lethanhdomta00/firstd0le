package com.erp.workflow.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDto {

    private Long id;
    private String traceId;
    private String username;
    private String userRole;
    private String action;
    private String entityType;
    private Long entityId;
    private String entityCode;
    private String oldValueJson;
    private String newValueJson;
    private String ipAddress;
    private String userAgent;
    private String reason;
    private LocalDateTime timestamp;
}
