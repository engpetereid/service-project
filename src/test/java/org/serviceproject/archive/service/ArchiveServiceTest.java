package org.serviceproject.archive.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.archive.dto.ArchiveSummaryResponse;
import org.serviceproject.archive.dto.DeletedPersonResponse;
import org.serviceproject.attendance.repository.AttendanceSessionRepository;
import org.serviceproject.audit.repository.AuditLogRepository;
import org.serviceproject.common.config.AppProperties;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.RoleWithScope;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.confession.repository.ConfessionRecordRepository;
import org.serviceproject.users.entity.Gender;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.Role;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.repository.PersonRepository;
import org.serviceproject.users.repository.UserAccountRepository;
import org.serviceproject.visits.repository.VisitRecordRepository;
import org.serviceproject.weeks.dto.WeekResponse;
import org.serviceproject.weeks.entity.Week;
import org.serviceproject.weeks.repository.WeekRepository;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArchiveServiceTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private WeekRepository weekRepository;

    @Mock
    private VisitRecordRepository visitRecordRepository;

    @Mock
    private AttendanceSessionRepository attendanceSessionRepository;

    @Mock
    private ConfessionRecordRepository confessionRecordRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private ArchiveService archiveService;

    private UserPrincipal adminPrincipal;
    private UserPrincipal servantPrincipal;
    private Person deletedPerson;
    private Week oldWeek;
    private Week recentWeek;

    @BeforeEach
    void setUp() {
        adminPrincipal = new UserPrincipal(1L, 10L, "01000000000", "pass", true, 0,
                Set.of(new RoleWithScope(Role.GENERAL_ADMIN, null, null)));

        servantPrincipal = new UserPrincipal(2L, 20L, "01001111111", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVANT, null, null)));

        deletedPerson = new Person();
        deletedPerson.setId(101L);
        deletedPerson.setFullName("مارك سامح");
        deletedPerson.setPhone("01234567890");
        deletedPerson.setGender(Gender.MALE);
        deletedPerson.setDeletedAt(LocalDateTime.now().minusDays(10));

        // Week from 45 days ago -> locked (since > 30 days)
        oldWeek = new Week(LocalDate.now().minusDays(51), LocalDate.now().minusDays(45));
        oldWeek.setId(1L);

        // Recent week from 5 days ago -> not locked
        recentWeek = new Week(LocalDate.now().minusDays(11), LocalDate.now().minusDays(5));
        recentWeek.setId(2L);
    }

    @Test
    void getDeletedPeople_admin_returnsList() {
        when(personRepository.findAllByDeletedAtIsNotNull()).thenReturn(List.of(deletedPerson));
        when(userAccountRepository.findByPersonId(101L)).thenReturn(Optional.empty()); // is a student

        List<DeletedPersonResponse> responses = archiveService.getDeletedPeople(adminPrincipal);

        assertEquals(1, responses.size());
        assertEquals("مارك سامح", responses.get(0).fullName());
        assertEquals("مخدوم", responses.get(0).personType());
    }

    @Test
    void getDeletedPeople_servant_identifiesAsServant() {
        when(personRepository.findAllByDeletedAtIsNotNull()).thenReturn(List.of(deletedPerson));
        when(userAccountRepository.findByPersonId(101L)).thenReturn(Optional.of(new UserAccount())); // has account -> servant

        List<DeletedPersonResponse> responses = archiveService.getDeletedPeople(adminPrincipal);

        assertEquals(1, responses.size());
        assertEquals("خادم", responses.get(0).personType());
    }

    @Test
    void getDeletedPeople_nonAdmin_throwsForbidden() {
        AppException ex = assertThrows(AppException.class, () ->
                archiveService.getDeletedPeople(servantPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    @Test
    void getLockedWeeks_returnsLockedWeeksOnly() {
        when(weekRepository.findAll()).thenReturn(List.of(oldWeek, recentWeek));
        when(appProperties.weekLockDays()).thenReturn(30);
        when(appProperties.timeZone()).thenReturn("Africa/Cairo");

        List<WeekResponse> lockedWeeks = archiveService.getLockedWeeks(servantPrincipal);

        assertEquals(1, lockedWeeks.size());
        assertEquals(1L, lockedWeeks.get(0).id());
        assertTrue(lockedWeeks.get(0).locked());
    }

    @Test
    void getArchiveSummary_admin_includesDeletedAndAudit() {
        when(personRepository.findAllByDeletedAtIsNotNull()).thenReturn(List.of(deletedPerson));
        when(auditLogRepository.count()).thenReturn(150L);
        when(weekRepository.findAll()).thenReturn(List.of(oldWeek, recentWeek));
        when(appProperties.weekLockDays()).thenReturn(30);
        when(appProperties.timeZone()).thenReturn("Africa/Cairo");
        when(visitRecordRepository.count()).thenReturn(500L);
        when(attendanceSessionRepository.count()).thenReturn(60L);
        when(confessionRecordRepository.count()).thenReturn(80L);

        ArchiveSummaryResponse summary = archiveService.getArchiveSummary(adminPrincipal);

        assertEquals(1L, summary.deletedPeopleCount());
        assertEquals(150L, summary.auditLogsCount());
        assertEquals(1L, summary.lockedWeeksCount());
        assertEquals(500L, summary.totalVisitsCount());
        assertEquals(60L, summary.totalAttendanceSessionsCount());
        assertEquals(80L, summary.totalConfessionsCount());
    }

    @Test
    void getArchiveSummary_nonAdmin_nullsDeletedAndAudit() {
        when(weekRepository.findAll()).thenReturn(List.of(oldWeek, recentWeek));
        when(appProperties.weekLockDays()).thenReturn(30);
        when(appProperties.timeZone()).thenReturn("Africa/Cairo");
        when(visitRecordRepository.count()).thenReturn(500L);
        when(attendanceSessionRepository.count()).thenReturn(60L);
        when(confessionRecordRepository.count()).thenReturn(80L);

        ArchiveSummaryResponse summary = archiveService.getArchiveSummary(servantPrincipal);

        assertNull(summary.deletedPeopleCount());
        assertNull(summary.auditLogsCount());
        assertEquals(1L, summary.lockedWeeksCount());
        verify(personRepository, never()).findAllByDeletedAtIsNotNull();
        verify(auditLogRepository, never()).count();
    }
}
