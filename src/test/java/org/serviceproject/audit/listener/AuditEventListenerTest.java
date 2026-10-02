package org.serviceproject.audit.listener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.audit.entity.AuditAction;
import org.serviceproject.audit.event.AuditEvent;
import org.serviceproject.audit.service.AuditService;
import org.serviceproject.common.security.RoleWithScope;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.users.entity.Role;

import java.util.Set;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditEventListenerTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuditEventListener auditEventListener;

    @Test
    void handleAuditEvent_dispatchesToAuditService() {
        UserPrincipal principal = new UserPrincipal(101L, 999L, "01000000000", "pass", true, 0,
                Set.of(new RoleWithScope(Role.GENERAL_ADMIN, null, null)));

        AuditEvent event = new AuditEvent(
                principal,
                AuditAction.ROLE_CHANGE,
                "UserAccount",
                201L,
                "oldRoles",
                "newRoles",
                "10.0.0.1"
        );

        auditEventListener.handleAuditEvent(event);

        verify(auditService).log(
                principal,
                AuditAction.ROLE_CHANGE,
                "UserAccount",
                201L,
                "oldRoles",
                "newRoles",
                "10.0.0.1"
        );
    }
}
