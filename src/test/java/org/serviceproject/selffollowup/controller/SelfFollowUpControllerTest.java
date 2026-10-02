package org.serviceproject.selffollowup.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.common.security.RoleWithScope;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.selffollowup.dto.SelfFollowUpCurrentWeekResponse;
import org.serviceproject.selffollowup.dto.SelfFollowUpRequest;
import org.serviceproject.selffollowup.dto.SelfFollowUpResponse;
import org.serviceproject.selffollowup.dto.SelfFollowUpStatsResponse;
import org.serviceproject.selffollowup.service.SelfFollowUpService;
import org.serviceproject.users.entity.Role;
import org.serviceproject.weeks.dto.WeekResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SelfFollowUpControllerTest {

    @Mock
    private SelfFollowUpService selfFollowUpService;

    @InjectMocks
    private SelfFollowUpController selfFollowUpController;

    private UserPrincipal servantPrincipal;
    private SelfFollowUpResponse sampleResponse;

    @BeforeEach
    void setUp() {
        servantPrincipal = new UserPrincipal(501L, 1001L, "01234567890", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVANT, null, null)));

        sampleResponse = new SelfFollowUpResponse(
                1L, 10L, LocalDate.of(2026, 9, 25), LocalDate.of(2026, 10, 1),
                false, 1L, "2026/2027", 18, 21,
                true, true, true, false, 82.9,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    void getCurrentWeek_returnsOkWithData() {
        WeekResponse weekResponse = new WeekResponse(10L, LocalDate.of(2026, 9, 25), LocalDate.of(2026, 10, 1), false, true);
        SelfFollowUpCurrentWeekResponse payload = new SelfFollowUpCurrentWeekResponse(weekResponse, sampleResponse, 21);

        when(selfFollowUpService.getCurrentWeek(servantPrincipal)).thenReturn(payload);

        ResponseEntity<SelfFollowUpCurrentWeekResponse> response = selfFollowUpController.getCurrentWeek(servantPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10L, response.getBody().week().id());
        assertNotNull(response.getBody().record());
        assertEquals(18, response.getBody().record().noteScore());
    }

    @Test
    void upsert_returnsOkWithSavedRecord() {
        SelfFollowUpRequest request = new SelfFollowUpRequest(10L, 18, true, true, true, false);

        when(selfFollowUpService.upsert(request, servantPrincipal)).thenReturn(sampleResponse);

        ResponseEntity<SelfFollowUpResponse> response = selfFollowUpController.upsert(request, servantPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        assertEquals(18, response.getBody().noteScore());
        verify(selfFollowUpService).upsert(request, servantPrincipal);
    }

    @Test
    void getByWeek_returnsOkWithRecord() {
        when(selfFollowUpService.getByWeek(10L, servantPrincipal)).thenReturn(sampleResponse);

        ResponseEntity<SelfFollowUpResponse> response = selfFollowUpController.getByWeek(10L, servantPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10L, response.getBody().weekId());
    }

    @Test
    void getHistory_returnsOkWithList() {
        when(selfFollowUpService.getHistory(servantPrincipal)).thenReturn(List.of(sampleResponse));

        ResponseEntity<List<SelfFollowUpResponse>> response = selfFollowUpController.getHistory(servantPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getStatistics_returnsOkWithStats() {
        SelfFollowUpStatsResponse stats = new SelfFollowUpStatsResponse(
                4, 3, 75.0, 85.0, 100.0, 66.7, 33.3, 0.0, 77.5, List.of()
        );

        when(selfFollowUpService.getStatistics(servantPrincipal)).thenReturn(stats);

        ResponseEntity<SelfFollowUpStatsResponse> response = selfFollowUpController.getStatistics(servantPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(4, response.getBody().totalWeeks());
        assertEquals(3, response.getBody().recordedWeeks());
        assertEquals(75.0, response.getBody().recordingRate());
    }
}
