package org.serviceproject.selffollowup.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.academic.service.AcademicYearService;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.RoleWithScope;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.selffollowup.dto.SelfFollowUpCurrentWeekResponse;
import org.serviceproject.selffollowup.dto.SelfFollowUpRequest;
import org.serviceproject.selffollowup.dto.SelfFollowUpResponse;
import org.serviceproject.selffollowup.dto.SelfFollowUpStatsResponse;
import org.serviceproject.selffollowup.entity.ServantWeeklyFollowUp;
import org.serviceproject.selffollowup.repository.ServantWeeklyFollowUpRepository;
import org.serviceproject.settings.service.SettingsService;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.Role;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.repository.UserAccountRepository;
import org.serviceproject.weeks.dto.WeekResponse;
import org.serviceproject.weeks.entity.Week;
import org.serviceproject.weeks.repository.WeekRepository;
import org.serviceproject.weeks.service.WeekService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SelfFollowUpServiceTest {

    @Mock
    private ServantWeeklyFollowUpRepository servantWeeklyFollowUpRepository;

    @Mock
    private WeekService weekService;

    @Mock
    private WeekRepository weekRepository;

    @Mock
    private AcademicYearService academicYearService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private SettingsService settingsService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SelfFollowUpService selfFollowUpService;

    private AcademicYear academicYear;
    private Week currentWeek;
    private Week pastWeek;
    private UserAccount servantAccount;
    private UserPrincipal servantPrincipal;
    private UserPrincipal classSecretaryPrincipal;
    private UserPrincipal unauthorizedPrincipal;

    @BeforeEach
    void setUp() {
        academicYear = new AcademicYear("2026/2027", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 8, 31), true);
        academicYear.setId(1L);

        currentWeek = new Week(LocalDate.of(2026, 9, 25), LocalDate.of(2026, 10, 1));
        currentWeek.setId(10L);

        pastWeek = new Week(LocalDate.of(2026, 8, 7), LocalDate.of(2026, 8, 13));
        pastWeek.setId(5L);

        Person servantPerson = new Person();
        servantPerson.setId(1001L);
        servantPerson.setFullName("مينا إبراهيم");
        servantPerson.setPhone("01234567890");

        servantAccount = new UserAccount();
        servantAccount.setId(501L);
        servantAccount.setPerson(servantPerson);

        servantPrincipal = new UserPrincipal(501L, 1001L, "01234567890", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVANT, null, null)));

        classSecretaryPrincipal = new UserPrincipal(502L, 1002L, "01234567891", "pass", true, 0,
                Set.of(new RoleWithScope(Role.CLASS_SECRETARY, null, 10L)));

        unauthorizedPrincipal = new UserPrincipal(503L, 1003L, "01234567892", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVICE_SECRETARY, 100L, null)));
    }

    @Test
    void getCurrentWeek_servant_existingRecord_returnsRecordAndMaxScore() {
        WeekResponse weekResponse = new WeekResponse(10L, currentWeek.getStartDate(), currentWeek.getEndDate(), false, true);
        when(weekService.getCurrentWeek()).thenReturn(weekResponse);
        when(weekService.getCurrentWeekEntity()).thenReturn(currentWeek);
        when(settingsService.getMaxNoteScore()).thenReturn(21);

        ServantWeeklyFollowUp record = new ServantWeeklyFollowUp(
                servantAccount, currentWeek, academicYear, 18, 21, true, true, false, null
        );
        record.setId(1L);
        when(servantWeeklyFollowUpRepository.findByUserIdAndWeekId(501L, 10L)).thenReturn(Optional.of(record));
        when(weekService.isWeekLockedForUser(currentWeek, servantPrincipal)).thenReturn(false);

        SelfFollowUpCurrentWeekResponse result = selfFollowUpService.getCurrentWeek(servantPrincipal);

        assertNotNull(result);
        assertEquals(10L, result.week().id());
        assertEquals(21, result.maxNoteScore());
        assertNotNull(result.record());
        assertEquals(18, result.record().noteScore());
        assertTrue(result.record().attendedMass());
        assertTrue(result.record().attendedServiceMeeting());
        assertFalse(result.record().attendedTasbeha());
        assertNull(result.record().attendedManagementMeeting());
        // 4 metrics filled: Mass (100) + ServiceMeeting (100) + Tasbeha (0) + Note (18/21*100 = 85.71) => avg 71.4
        assertEquals(71.4, result.record().overallPercentage());
    }

    @Test
    void getCurrentWeek_servant_noRecord_returnsNullRecord() {
        WeekResponse weekResponse = new WeekResponse(10L, currentWeek.getStartDate(), currentWeek.getEndDate(), false, true);
        when(weekService.getCurrentWeek()).thenReturn(weekResponse);
        when(weekService.getCurrentWeekEntity()).thenReturn(currentWeek);
        when(settingsService.getMaxNoteScore()).thenReturn(21);
        when(servantWeeklyFollowUpRepository.findByUserIdAndWeekId(501L, 10L)).thenReturn(Optional.empty());

        SelfFollowUpCurrentWeekResponse result = selfFollowUpService.getCurrentWeek(servantPrincipal);

        assertNotNull(result);
        assertEquals(10L, result.week().id());
        assertEquals(21, result.maxNoteScore());
        assertNull(result.record());
    }

    @Test
    void getCurrentWeek_unauthorizedRole_throwsForbidden() {
        AppException ex = assertThrows(AppException.class, () ->
                selfFollowUpService.getCurrentWeek(unauthorizedPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    @Test
    void upsert_createsNewRecord_freezesMaxNoteScoreSnapshot_success() {
        SelfFollowUpRequest request = new SelfFollowUpRequest(10L, 15, true, true, true, false);

        when(weekService.getWeekOrThrow(10L)).thenReturn(currentWeek);
        when(weekService.isWeekLockedForUser(currentWeek, servantPrincipal)).thenReturn(false);
        when(servantWeeklyFollowUpRepository.findByUserIdAndWeekId(501L, 10L)).thenReturn(Optional.empty());
        when(settingsService.getMaxNoteScore()).thenReturn(21);
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(userAccountRepository.findById(501L)).thenReturn(Optional.of(servantAccount));

        when(servantWeeklyFollowUpRepository.save(any(ServantWeeklyFollowUp.class))).thenAnswer(invocation -> {
            ServantWeeklyFollowUp saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        SelfFollowUpResponse response = selfFollowUpService.upsert(request, servantPrincipal);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(15, response.noteScore());
        assertEquals(21, response.maxNoteScoreSnapshot());
        assertTrue(response.attendedMass());
        assertTrue(response.attendedServiceMeeting());
        assertTrue(response.attendedTasbeha());
        assertFalse(response.attendedManagementMeeting());
        // 5 metrics: 100 + 100 + 100 + 0 + (15/21*100 = 71.43) => avg 74.3
        assertEquals(74.3, response.overallPercentage());
        verify(servantWeeklyFollowUpRepository).save(any(ServantWeeklyFollowUp.class));
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void upsert_updatesExistingRecord_preservesSnapshot_success() {
        SelfFollowUpRequest request = new SelfFollowUpRequest(10L, 14, false, true, true, null);

        when(weekService.getWeekOrThrow(10L)).thenReturn(currentWeek);
        when(weekService.isWeekLockedForUser(currentWeek, classSecretaryPrincipal)).thenReturn(false);

        // Suppose snapshot was 25 in the past
        ServantWeeklyFollowUp existing = new ServantWeeklyFollowUp(
                servantAccount, currentWeek, academicYear, 10, 25, true, false, false, false
        );
        existing.setId(99L);

        when(servantWeeklyFollowUpRepository.findByUserIdAndWeekId(502L, 10L)).thenReturn(Optional.of(existing));
        when(servantWeeklyFollowUpRepository.save(any(ServantWeeklyFollowUp.class))).thenAnswer(i -> i.getArgument(0));

        SelfFollowUpResponse response = selfFollowUpService.upsert(request, classSecretaryPrincipal);

        assertNotNull(response);
        assertEquals(99L, response.id());
        assertEquals(14, response.noteScore());
        assertEquals(25, response.maxNoteScoreSnapshot()); // Preserved!
        assertFalse(response.attendedMass());
        assertTrue(response.attendedServiceMeeting());
        assertTrue(response.attendedTasbeha());
        assertNull(response.attendedManagementMeeting());
        // 4 metrics: 0 + 100 + 100 + (14/25*100 = 56.0) => avg 64.0
        assertEquals(64.0, response.overallPercentage());
        verify(settingsService, never()).getMaxNoteScore();
    }

    @Test
    void upsert_lockedWeek_throwsForbidden() {
        SelfFollowUpRequest request = new SelfFollowUpRequest(5L, 10, true, null, null, null);

        when(weekService.getWeekOrThrow(5L)).thenReturn(pastWeek);
        when(weekService.isWeekLockedForUser(pastWeek, servantPrincipal)).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () ->
                selfFollowUpService.upsert(request, servantPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("WEEK_LOCKED", ex.getCode());
        verify(servantWeeklyFollowUpRepository, never()).save(any());
    }

    @Test
    void upsert_noteScoreExceedsMax_throwsBadRequest() {
        SelfFollowUpRequest request = new SelfFollowUpRequest(10L, 25, true, null, null, null);

        when(weekService.getWeekOrThrow(10L)).thenReturn(currentWeek);
        when(weekService.isWeekLockedForUser(currentWeek, servantPrincipal)).thenReturn(false);
        when(servantWeeklyFollowUpRepository.findByUserIdAndWeekId(501L, 10L)).thenReturn(Optional.empty());
        when(settingsService.getMaxNoteScore()).thenReturn(21);

        AppException ex = assertThrows(AppException.class, () ->
                selfFollowUpService.upsert(request, servantPrincipal));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_NOTE_SCORE", ex.getCode());
    }

    @Test
    void upsert_negativeNoteScore_throwsBadRequest() {
        SelfFollowUpRequest request = new SelfFollowUpRequest(10L, -5, true, null, null, null);

        when(weekService.getWeekOrThrow(10L)).thenReturn(currentWeek);
        when(weekService.isWeekLockedForUser(currentWeek, servantPrincipal)).thenReturn(false);
        when(servantWeeklyFollowUpRepository.findByUserIdAndWeekId(501L, 10L)).thenReturn(Optional.empty());
        when(settingsService.getMaxNoteScore()).thenReturn(21);

        AppException ex = assertThrows(AppException.class, () ->
                selfFollowUpService.upsert(request, servantPrincipal));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_NOTE_SCORE", ex.getCode());
    }

    @Test
    void getByWeek_existingRecord_returnsResponse() {
        when(weekService.getWeekOrThrow(10L)).thenReturn(currentWeek);
        ServantWeeklyFollowUp record = new ServantWeeklyFollowUp(
                servantAccount, currentWeek, academicYear, 20, 21, true, true, true, true
        );
        record.setId(10L);
        when(servantWeeklyFollowUpRepository.findByUserIdAndWeekId(501L, 10L)).thenReturn(Optional.of(record));

        SelfFollowUpResponse response = selfFollowUpService.getByWeek(10L, servantPrincipal);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(20, response.noteScore());
    }

    @Test
    void getByWeek_recordNotFound_throwsNotFound() {
        when(weekService.getWeekOrThrow(10L)).thenReturn(currentWeek);
        when(servantWeeklyFollowUpRepository.findByUserIdAndWeekId(501L, 10L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                selfFollowUpService.getByWeek(10L, servantPrincipal));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("FOLLOWUP_NOT_FOUND", ex.getCode());
    }

    @Test
    void getHistory_returnsAllRecordsForCurrentYear() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);

        ServantWeeklyFollowUp r1 = new ServantWeeklyFollowUp(servantAccount, currentWeek, academicYear, 15, 21, true, null, null, null);
        r1.setId(1L);
        ServantWeeklyFollowUp r2 = new ServantWeeklyFollowUp(servantAccount, pastWeek, academicYear, 20, 21, false, true, null, null);
        r2.setId(2L);

        when(servantWeeklyFollowUpRepository.findAllByUserIdAndAcademicYearIdOrderByWeekStartDateDesc(501L, 1L))
                .thenReturn(List.of(r1, r2));

        List<SelfFollowUpResponse> history = selfFollowUpService.getHistory(servantPrincipal);

        assertEquals(2, history.size());
        assertEquals(1L, history.get(0).id());
        assertEquals(2L, history.get(1).id());
    }

    @Test
    void getStatistics_calculatesAccuratePercentagesAndTrend() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);

        // 3 total weeks in academic year
        Week w1 = new Week(LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 10));
        w1.setId(1L);
        Week w2 = new Week(LocalDate.of(2026, 9, 11), LocalDate.of(2026, 9, 17));
        w2.setId(2L);
        Week w3 = new Week(LocalDate.of(2026, 9, 18), LocalDate.of(2026, 9, 24));
        w3.setId(3L);

        when(weekRepository.findAllByDeletedAtIsNullOrderByStartDateDesc()).thenReturn(List.of(w3, w2, w1));

        // 2 recorded weeks
        ServantWeeklyFollowUp rec1 = new ServantWeeklyFollowUp(servantAccount, w1, academicYear, 21, 21, true, true, false, true);
        rec1.setId(101L);
        // rec1 scores: Mass=100, Service=100, Tasbeha=0, Mgmt=100, Note=21/21=100 => avg 80.0

        ServantWeeklyFollowUp rec2 = new ServantWeeklyFollowUp(servantAccount, w2, academicYear, 10, 20, false, true, true, null);
        rec2.setId(102L);
        // rec2 scores: Mass=0, Service=100, Tasbeha=100, Mgmt=null, Note=10/20=50 => avg 62.5

        when(servantWeeklyFollowUpRepository.findAllByUserIdAndAcademicYearIdOrderByWeekStartDateAsc(501L, 1L))
                .thenReturn(List.of(rec1, rec2));

        SelfFollowUpStatsResponse stats = selfFollowUpService.getStatistics(servantPrincipal);

        assertNotNull(stats);
        assertEquals(3L, stats.totalWeeks());
        assertEquals(2L, stats.recordedWeeks());
        assertEquals(66.7, stats.recordingRate()); // 2 / 3 * 100 = 66.7%

        // Note: rec1 = 100%, rec2 = 50% => avg = 75.0%
        assertEquals(75.0, stats.avgNotePercentage());

        // Mass: rec1=true, rec2=false => 1/2 = 50.0%
        assertEquals(50.0, stats.avgMassRate());

        // ServiceMeeting: rec1=true, rec2=true => 2/2 = 100.0%
        assertEquals(100.0, stats.avgServiceMeetingRate());

        // Tasbeha: rec1=false, rec2=true => 1/2 = 50.0%
        assertEquals(50.0, stats.avgTasbehaRate());

        // ManagementMeeting: rec1=true, rec2=null => 1/1 = 100.0%
        assertEquals(100.0, stats.avgManagementMeetingRate());

        // Overall: (80.0 + 62.5) / 2 = 71.3%
        assertEquals(71.3, stats.avgOverallPercentage());

        // Trend points
        assertEquals(2, stats.weeklyTrend().size());
        assertEquals(1L, stats.weeklyTrend().get(0).weekId());
        assertEquals(80.0, stats.weeklyTrend().get(0).overallPercentage());
        assertEquals(2L, stats.weeklyTrend().get(1).weekId());
        assertEquals(62.5, stats.weeklyTrend().get(1).overallPercentage());
    }

    @Test
    void calculateOverallPercentage_allNull_returnsZero() {
        Double percent = SelfFollowUpService.calculateOverallPercentage(null, 21, null, null, null, null);
        assertEquals(0.0, percent);
    }
}
