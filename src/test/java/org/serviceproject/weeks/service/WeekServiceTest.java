package org.serviceproject.weeks.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.common.config.AppProperties;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.RoleWithScope;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.users.entity.Role;
import org.serviceproject.weeks.dto.WeekRequest;
import org.serviceproject.weeks.dto.WeekResponse;
import org.serviceproject.weeks.entity.Week;
import org.serviceproject.weeks.repository.WeekRepository;
import org.springframework.http.HttpStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeekServiceTest {

    @Mock
    private WeekRepository weekRepository;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private WeekService weekService;

    private Week week;
    private UserPrincipal adminPrincipal;
    private UserPrincipal servantPrincipal;

    @BeforeEach
    void setUp() {
        lenient().when(appProperties.timeZone()).thenReturn("Africa/Cairo");
        lenient().when(appProperties.weekLockDays()).thenReturn(30);

        week = new Week(LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 10)); // Friday to Thursday
        week.setId(1L);

        adminPrincipal = new UserPrincipal(1L, 999L, "01000000000", "pass", true, 0,
                Set.of(new RoleWithScope(Role.GENERAL_ADMIN, null, null)));

        servantPrincipal = new UserPrincipal(2L, 888L, "01000000001", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVANT, null, null)));
    }

    @Test
    void getCurrentWeek_autoCreatesIfMissing() {
        when(weekRepository.findByStartDate(any())).thenReturn(Optional.empty());
        when(weekRepository.save(any(Week.class))).thenAnswer(i -> {
            Week w = i.getArgument(0);
            w.setId(10L);
            return w;
        });

        WeekResponse response = weekService.getCurrentWeek();

        assertNotNull(response);
        assertEquals(10L, response.id());
        verify(weekRepository).save(any(Week.class));
    }

    @Test
    void ensureWeek_returnsExistingIfPresent() {
        LocalDate startDate = LocalDate.of(2026, 9, 4);
        when(weekRepository.findByStartDate(startDate)).thenReturn(Optional.of(week));

        Week result = weekService.ensureWeek(startDate);

        assertEquals(week, result);
        verify(weekRepository, never()).save(any());
    }

    @Test
    void ensureWeek_restoresIfSoftDeleted() {
        LocalDate startDate = LocalDate.of(2026, 9, 4);
        week.softDelete();
        when(weekRepository.findByStartDate(startDate)).thenReturn(Optional.of(week));
        when(weekRepository.save(week)).thenReturn(week);

        Week result = weekService.ensureWeek(startDate);

        assertFalse(result.isDeleted());
        verify(weekRepository).save(week);
    }

    @Test
    void findAll_returnsList() {
        when(weekRepository.findAllByDeletedAtIsNullOrderByStartDateDesc()).thenReturn(List.of(week));

        List<WeekResponse> list = weekService.findAll(false);

        assertEquals(1, list.size());
        assertEquals(1L, list.get(0).id());
    }

    @Test
    void findById_success() {
        when(weekRepository.findById(1L)).thenReturn(Optional.of(week));

        WeekResponse res = weekService.findById(1L);

        assertNotNull(res);
        assertEquals(1L, res.id());
    }

    @Test
    void findById_notFound_throwsException() {
        when(weekRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> weekService.findById(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void create_success() {
        WeekRequest request = new WeekRequest(LocalDate.of(2026, 9, 11), LocalDate.of(2026, 9, 17));
        when(weekRepository.existsByStartDate(request.startDate())).thenReturn(false);
        when(weekRepository.save(any(Week.class))).thenAnswer(i -> {
            Week w = i.getArgument(0);
            w.setId(2L);
            return w;
        });

        WeekResponse res = weekService.create(request);

        assertNotNull(res);
        assertEquals(LocalDate.of(2026, 9, 11), res.startDate());
    }

    @Test
    void create_invalidDates_throwsBadRequest() {
        WeekRequest request = new WeekRequest(LocalDate.of(2026, 9, 18), LocalDate.of(2026, 9, 11));

        AppException ex = assertThrows(AppException.class, () -> weekService.create(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_DATES", ex.getCode());
    }

    @Test
    void create_duplicateStartDate_throwsConflict() {
        WeekRequest request = new WeekRequest(LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 10));
        when(weekRepository.existsByStartDate(request.startDate())).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> weekService.create(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("WEEK_EXISTS", ex.getCode());
    }

    @Test
    void update_success() {
        WeekRequest request = new WeekRequest(LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 11));
        when(weekRepository.findById(1L)).thenReturn(Optional.of(week));
        when(weekRepository.save(any(Week.class))).thenReturn(week);

        WeekResponse res = weekService.update(1L, request);

        assertNotNull(res);
        assertEquals(LocalDate.of(2026, 9, 11), week.getEndDate());
    }

    @Test
    void softDelete_success() {
        when(weekRepository.findById(1L)).thenReturn(Optional.of(week));

        weekService.softDelete(1L);

        assertTrue(week.isDeleted());
        verify(weekRepository).save(week);
    }

    @Test
    void restore_success() {
        week.softDelete();
        when(weekRepository.findById(1L)).thenReturn(Optional.of(week));

        weekService.restore(1L);

        assertFalse(week.isDeleted());
        verify(weekRepository).save(week);
    }

    @Test
    void restore_notDeleted_throwsBadRequest() {
        when(weekRepository.findById(1L)).thenReturn(Optional.of(week));

        AppException ex = assertThrows(AppException.class, () -> weekService.restore(1L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("NOT_DELETED", ex.getCode());
    }

    // ── Dynamic Lock & Admin Override Tests ───────────────────────────

    @Test
    void isWeekLockedForUser_adminBypassesLock() {
        // Very old week (definitely locked by date calculation)
        Week oldWeek = new Week(LocalDate.of(2020, 1, 3), LocalDate.of(2020, 1, 9));

        boolean isLocked = weekService.isWeekLockedForUser(oldWeek, adminPrincipal);

        assertFalse(isLocked, "Admin must bypass all week locks");
    }

    @Test
    void isWeekLockedForUser_servantRespectsLock() {
        // Very old week (definitely locked by date calculation)
        Week oldWeek = new Week(LocalDate.of(2020, 1, 3), LocalDate.of(2020, 1, 9));

        boolean isLocked = weekService.isWeekLockedForUser(oldWeek, servantPrincipal);

        assertTrue(isLocked, "Servant must be subject to the 30-day dynamic lock");
    }

    @Test
    void isWeekLockedForUser_servantRecentWeekNotLocked() {
        // Recent / future week
        Week futureWeek = new Week(LocalDate.now().plusWeeks(1), LocalDate.now().plusWeeks(2));

        boolean isLocked = weekService.isWeekLockedForUser(futureWeek, servantPrincipal);

        assertFalse(isLocked, "Future/recent week should not be locked");
    }
}
