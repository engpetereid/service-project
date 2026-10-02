package org.serviceproject.audit.listener;

import lombok.RequiredArgsConstructor;
import org.serviceproject.audit.event.AuditEvent;
import org.serviceproject.audit.service.AuditService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event listener that persists audit events asynchronously or synchronously.
 */
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditService auditService;

    @EventListener
    public void handleAuditEvent(AuditEvent event) {
        auditService.log(
                event.principal(),
                event.action(),
                event.entityType(),
                event.entityId(),
                event.oldValues(),
                event.newValues(),
                event.clientIp()
        );
    }
}
