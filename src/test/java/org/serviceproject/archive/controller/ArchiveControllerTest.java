package org.serviceproject.archive.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.archive.dto.ArchiveSummaryResponse;
import org.serviceproject.archive.dto.DeletedPersonResponse;
import org.serviceproject.archive.service.ArchiveService;
import org.serviceproject.common.security.RoleWithScope;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.users.entity.Gender;
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
class ArchiveControllerTest {

    @Mock
    private ArchiveService archiveService;

    @InjectMocks
    private ArchiveController archiveController;

    private UserPrincipal adminPrincipal;
    private DeletedPersonResponse deletedPersonResponse;

    @BeforeEach
    void setUp() {
        adminPrincipal = new UserPrincipal(1L, 10L, "01000000000", "pass", true, 0,
                Set.of(new RoleWithScope(Role.GENERAL_ADMIN, null, null)));

        deletedPersonResponse = new DeletedPersonResponse(
                101L, "مارك سامح", "01234567890", Gender.MALE, "مخدوم",
                LocalDateTime.now(), "ابتدائي", "رابعة ابتدائي"
        );
    }

    @Test
    void getDeletedPeople_returnsOkWithList() {
        when(archiveService.getDeletedPeople(adminPrincipal)).thenReturn(List.of(deletedPersonResponse));

        ResponseEntity<List<DeletedPersonResponse>> response = archiveController.getDeletedPeople(adminPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("مارك سامح", response.getBody().get(0).fullName());
    }

    @Test
    void getLockedWeeks_returnsOkWithList() {
        WeekResponse weekResponse = new WeekResponse(1L, LocalDate.now().minusWeeks(5), LocalDate.now().minusWeeks(4), true, true);
        when(archiveService.getLockedWeeks(adminPrincipal)).thenReturn(List.of(weekResponse));

        ResponseEntity<List<WeekResponse>> response = archiveController.getLockedWeeks(adminPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertTrue(response.getBody().get(0).locked());
    }

    @Test
    void getArchiveSummary_returnsOkWithSummary() {
        ArchiveSummaryResponse summary = new ArchiveSummaryResponse(5L, 10L, 500L, 60L, 80L, 150L);
        when(archiveService.getArchiveSummary(adminPrincipal)).thenReturn(summary);

        ResponseEntity<ArchiveSummaryResponse> response = archiveController.getArchiveSummary(adminPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5L, response.getBody().deletedPeopleCount());
        assertEquals(10L, response.getBody().lockedWeeksCount());
    }
}
