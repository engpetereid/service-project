package org.serviceproject.audit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.audit.dto.AuditLogFilter;
import org.serviceproject.audit.dto.AuditLogResponse;
import org.serviceproject.audit.entity.AuditAction;
import org.serviceproject.audit.entity.AuditLog;
import org.serviceproject.audit.repository.AuditLogRepository;
import org.serviceproject.common.dto.PageResponse;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.RoleWithScope;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.users.entity.Gender;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.Role;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.repository.UserAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuditService auditService;

    private UserPrincipal adminPrincipal;
    private UserPrincipal servantPrincipal;
    private UserAccount adminAccount;
    private Person adminPerson;

    @BeforeEach
    void setUp() {
        adminPerson = new Person();
        adminPerson.setId(999L);
        adminPerson.setFullName("المدير العام");
        adminPerson.setPhone("01000000000");
        adminPerson.setGender(Gender.MALE);

        adminAccount = new UserAccount();
        adminAccount.setId(101L);
        adminAccount.setPerson(adminPerson);

        adminPrincipal = new UserPrincipal(101L, 999L, "01000000000", "pass", true, 0,
                Set.of(new RoleWithScope(Role.GENERAL_ADMIN, null, null)));

        servantPrincipal = new UserPrincipal(201L, 3001L, "01001111111", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVANT, null, null)));
    }

    @Test
    void log_withPrincipal_recordsAuditLogWithActorAndIp() throws Exception {
        when(userAccountRepository.findById(101L)).thenReturn(Optional.of(adminAccount));
        java.util.Map<String, String> oldObj = java.util.Map.of("role", "SERVANT");
        java.util.Map<String, String> newObj = java.util.Map.of("role", "GENERAL_ADMIN");
        when(objectMapper.writeValueAsString(oldObj)).thenReturn("{\"role\":\"SERVANT\"}");
        when(objectMapper.writeValueAsString(newObj)).thenReturn("{\"role\":\"GENERAL_ADMIN\"}");

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> {
            AuditLog al = i.getArgument(0);
            al.setId(1L);
            al.setCreatedAt(LocalDateTime.now());
            return al;
        });

        AuditLogResponse response = auditService.log(
                adminPrincipal, AuditAction.ROLE_CHANGE, "UserAccount", 201L, oldObj, newObj, "192.168.1.10"
        );

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(101L, response.actorUserId());
        assertEquals("المدير العام", response.actorName());
        assertEquals(AuditAction.ROLE_CHANGE, response.action());
        assertEquals("UserAccount", response.entityType());
        assertEquals(201L, response.entityId());
        assertEquals("{\"role\":\"SERVANT\"}", response.oldValues());
        assertEquals("{\"role\":\"GENERAL_ADMIN\"}", response.newValues());
        assertEquals("192.168.1.10", response.clientIp());

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void log_withSystem_recordsAuditLogWithSystemActor() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> {
            AuditLog al = i.getArgument(0);
            al.setId(2L);
            al.setCreatedAt(LocalDateTime.now());
            return al;
        });

        AuditLogResponse response = auditService.logSystem(
                AuditAction.PROMOTION, "AcademicYear", 5L, null, "Promoted: 10"
        );

        assertNotNull(response);
        assertEquals(2L, response.id());
        assertNull(response.actorUserId());
        assertEquals("SYSTEM", response.actorName());
        assertEquals(AuditAction.PROMOTION, response.action());
        assertEquals("AcademicYear", response.entityType());
        assertEquals(5L, response.entityId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findLogs_adminPrincipal_returnsFilteredPageResponse() {
        AuditLog logEntry = new AuditLog(adminAccount, "المدير العام", AuditAction.DELETE, "Student", 5001L,
                null, null, "127.0.0.1");
        logEntry.setId(10L);
        logEntry.setCreatedAt(LocalDateTime.now());

        Page<AuditLog> page = new PageImpl<>(List.of(logEntry));
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        AuditLogFilter filter = new AuditLogFilter("Student", 5001L, 101L, AuditAction.DELETE,
                LocalDate.now().minusDays(1), LocalDate.now(), 0, 10);

        PageResponse<AuditLogResponse> response = auditService.findLogs(filter, adminPrincipal);

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(10L, response.content().get(0).id());
        assertEquals(AuditAction.DELETE, response.content().get(0).action());
        assertEquals("Student", response.content().get(0).entityType());
        assertEquals(5001L, response.content().get(0).entityId());
        assertEquals(1, response.totalElements());
    }

    @Test
    void findLogs_nonAdminPrincipal_throwsForbidden() {
        AuditLogFilter filter = new AuditLogFilter(null, null, null, null, null, null, 0, 20);

        AppException ex = assertThrows(AppException.class, () ->
                auditService.findLogs(filter, servantPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
        verify(auditLogRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void findLogs_nullPrincipal_throwsForbidden() {
        AuditLogFilter filter = new AuditLogFilter(null, null, null, null, null, null, 0, 20);

        AppException ex = assertThrows(AppException.class, () ->
                auditService.findLogs(filter, null));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    @Test
    void log_serializationException_fallsBackToString() throws Exception {
        Object badObj = new Object();
        when(objectMapper.writeValueAsString(badObj)).thenThrow(new RuntimeException("JSON error"));

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> {
            AuditLog al = i.getArgument(0);
            al.setId(99L);
            al.setCreatedAt(LocalDateTime.now());
            return al;
        });

        AuditLogResponse response = auditService.log(
                null, AuditAction.UPDATE, "SystemSetting", 1L, badObj, null, null
        );

        assertNotNull(response);
        assertNotNull(response.oldValues());
        assertTrue(response.oldValues().contains("Object@"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findLogs_withEmptyFilter_executesQueryWithDefaults() {
        Page<AuditLog> emptyPage = new PageImpl<>(List.of());
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        AuditLogFilter filter = new AuditLogFilter(null, null, null, null, null, null, -5, 500);

        assertEquals(0, filter.getResolvedPage());
        assertEquals(20, filter.getResolvedSize()); // capped at 20 because > 100

        PageResponse<AuditLogResponse> response = auditService.findLogs(filter, adminPrincipal);

        assertNotNull(response);
        assertTrue(response.content().isEmpty());
    }
}
