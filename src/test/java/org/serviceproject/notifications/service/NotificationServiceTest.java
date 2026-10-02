package org.serviceproject.notifications.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.academic.service.AcademicYearService;
import org.serviceproject.classes.entity.GradeClass;
import org.serviceproject.common.config.AppProperties;
import org.serviceproject.common.dto.PageResponse;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.RoleWithScope;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.ministries.entity.Ministry;
import org.serviceproject.notifications.dto.NotificationResponse;
import org.serviceproject.notifications.dto.UnreadCountResponse;
import org.serviceproject.notifications.entity.Notification;
import org.serviceproject.notifications.entity.NotificationType;
import org.serviceproject.notifications.mapper.NotificationMapper;
import org.serviceproject.notifications.repository.NotificationRepository;
import org.serviceproject.staff.entity.StaffPlacement;
import org.serviceproject.staff.repository.StaffPlacementRepository;
import org.serviceproject.students.entity.StudentPlacement;
import org.serviceproject.students.entity.StudentStatus;
import org.serviceproject.students.repository.StudentPlacementRepository;
import org.serviceproject.users.entity.Gender;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.Role;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.repository.UserAccountRepository;
import org.serviceproject.visits.entity.VisitRecord;
import org.serviceproject.visits.repository.VisitRecordRepository;
import org.serviceproject.weeks.entity.Week;
import org.serviceproject.weeks.service.WeekService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private WeekService weekService;

    @Mock
    private AcademicYearService academicYearService;

    @Mock
    private StaffPlacementRepository staffPlacementRepository;

    @Mock
    private StudentPlacementRepository studentPlacementRepository;

    @Mock
    private VisitRecordRepository visitRecordRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private NotificationService notificationService;

    private UserAccount account1;
    private UserAccount account2;
    private Person servantPerson;
    private Person studentPerson1;
    private Person studentPerson2;
    private UserPrincipal principal1;
    private Notification notification1;
    private NotificationResponse notificationResponse1;
    private Week currentWeek;
    private AcademicYear academicYear;
    private Ministry ministry;
    private GradeClass gradeClass;

    @BeforeEach
    void setUp() {
        servantPerson = new Person();
        servantPerson.setId(10L);
        servantPerson.setFullName("مينا جرجس");
        servantPerson.setPhone("01001111111");
        servantPerson.setGender(Gender.MALE);

        account1 = new UserAccount();
        account1.setId(100L);
        account1.setPerson(servantPerson);
        account1.setEnabled(true);

        Person person2 = new Person();
        person2.setId(20L);
        person2.setFullName("بيتر بولس");
        person2.setPhone("01002222222");

        account2 = new UserAccount();
        account2.setId(200L);
        account2.setPerson(person2);
        account2.setEnabled(true);

        principal1 = new UserPrincipal(100L, 10L, "01001111111", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVANT, null, null)));

        notification1 = new Notification(account1, "تذكير", "لديك 3 مخدومين", NotificationType.WEEKLY_REMINDER, "WEEK_5");
        notification1.setId(501L);

        notificationResponse1 = new NotificationResponse(
                501L, "تذكير", "لديك 3 مخدومين", NotificationType.WEEKLY_REMINDER,
                false, null, "WEEK_5", LocalDateTime.now()
        );

        currentWeek = new Week(LocalDate.now().minusDays(3), LocalDate.now().plusDays(3));
        currentWeek.setId(5L);

        academicYear = new AcademicYear("2026/2027", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 8, 31), true);
        academicYear.setId(1L);

        ministry = new Ministry("ابتدائي");
        ministry.setId(10L);
        gradeClass = new GradeClass("رابعة ابتدائي", ministry);
        gradeClass.setId(100L);

        studentPerson1 = new Person();
        studentPerson1.setId(1001L);
        studentPerson1.setFullName("مارك سامح");

        studentPerson2 = new Person();
        studentPerson2.setId(1002L);
        studentPerson2.setFullName("مارينا يوسف");
    }

    @Test
    void getUserNotifications_all_returnsPageResponse() {
        Page<Notification> page = new PageImpl<>(List.of(notification1), PageRequest.of(0, 20), 1);
        when(notificationRepository.findAllByUserIdOrderByCreatedAtDesc(eq(100L), any())).thenReturn(page);
        when(notificationMapper.toResponse(notification1)).thenReturn(notificationResponse1);

        PageResponse<NotificationResponse> result = notificationService.getUserNotifications(principal1, false, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.totalElements());
        assertEquals("تذكير", result.content().get(0).title());
    }

    @Test
    void getUserNotifications_unreadOnly_callsUnreadQuery() {
        Page<Notification> page = new PageImpl<>(List.of(notification1), PageRequest.of(0, 20), 1);
        when(notificationRepository.findAllByUserIdAndReadFalseOrderByCreatedAtDesc(eq(100L), any())).thenReturn(page);
        when(notificationMapper.toResponse(notification1)).thenReturn(notificationResponse1);

        PageResponse<NotificationResponse> result = notificationService.getUserNotifications(principal1, true, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.totalElements());
        verify(notificationRepository).findAllByUserIdAndReadFalseOrderByCreatedAtDesc(eq(100L), any());
    }

    @Test
    void getUserNotifications_nullPrincipal_throwsUnauthorized() {
        AppException ex = assertThrows(AppException.class, () ->
                notificationService.getUserNotifications(null, false, 0, 20));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void getUnreadCount_returnsCount() {
        when(notificationRepository.countByUserIdAndReadFalse(100L)).thenReturn(4L);

        UnreadCountResponse response = notificationService.getUnreadCount(principal1);

        assertEquals(4L, response.unreadCount());
    }

    @Test
    void markAsRead_success_marksReadAndReturns() {
        when(notificationRepository.findById(501L)).thenReturn(Optional.of(notification1));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse readResponse = new NotificationResponse(
                501L, "تذكير", "لديك 3 مخدومين", NotificationType.WEEKLY_REMINDER,
                true, LocalDateTime.now(), "WEEK_5", LocalDateTime.now()
        );
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(readResponse);

        NotificationResponse result = notificationService.markAsRead(501L, principal1);

        assertTrue(result.read());
        assertTrue(notification1.isRead());
        assertNotNull(notification1.getReadAt());
    }

    @Test
    void markAsRead_notOwned_throwsForbidden() {
        notification1.setUser(account2); // Belongs to account 200, but principal is 100
        when(notificationRepository.findById(501L)).thenReturn(Optional.of(notification1));

        AppException ex = assertThrows(AppException.class, () ->
                notificationService.markAsRead(501L, principal1));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    @Test
    void markAsRead_notFound_throwsNotFound() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                notificationService.markAsRead(999L, principal1));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("NOTIFICATION_NOT_FOUND", ex.getCode());
    }

    @Test
    void markAllAsRead_callsRepoAndReturnsCount() {
        when(notificationRepository.markAllAsReadByUserId(eq(100L), any(LocalDateTime.class))).thenReturn(3);

        int updated = notificationService.markAllAsRead(principal1);

        assertEquals(3, updated);
        verify(notificationRepository).markAllAsReadByUserId(eq(100L), any(LocalDateTime.class));
    }

    @Test
    void createNotification_alreadyExists_returnsNull() {
        when(notificationRepository.existsByUserIdAndTypeAndReferenceId(100L, NotificationType.WEEKLY_REMINDER, "WEEK_5"))
                .thenReturn(true);

        Notification result = notificationService.createNotification(account1, "تذكير", "رسالة", NotificationType.WEEKLY_REMINDER, "WEEK_5");

        assertNull(result);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createNotification_new_savesAndReturns() {
        when(notificationRepository.existsByUserIdAndTypeAndReferenceId(100L, NotificationType.WEEKLY_REMINDER, "WEEK_6"))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        Notification result = notificationService.createNotification(account1, "تذكير", "رسالة", NotificationType.WEEKLY_REMINDER, "WEEK_6");

        assertNotNull(result);
        assertEquals("تذكير", result.getTitle());
        assertEquals(account1, result.getUser());
    }

    @Test
    void sendWeeklyReminders_outsideWindow_doesNotSend() {
        // Week ends in 3 days; outside the 24-hour reminder window
        when(weekService.getCurrentWeekEntity()).thenReturn(currentWeek);
        when(appProperties.timeZone()).thenReturn("Africa/Cairo");
        when(appProperties.weeklyReminderHours()).thenReturn(24);

        int count = notificationService.sendWeeklyReminders();

        assertEquals(0, count);
        verify(staffPlacementRepository, never()).findAllActiveByAcademicYearId(any());
    }

    @Test
    void triggerWeeklyRemindersManually_servantWithUnvisited_createsNotification() {
        when(weekService.getCurrentWeekEntity()).thenReturn(currentWeek);
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);

        StaffPlacement staffPlacement = new StaffPlacement(servantPerson, academicYear, ministry, gradeClass);
        when(staffPlacementRepository.findAllActiveByAcademicYearId(1L)).thenReturn(List.of(staffPlacement));
        when(userAccountRepository.findByPersonId(10L)).thenReturn(Optional.of(account1));

        // 2 assigned students
        StudentPlacement sp1 = new StudentPlacement(studentPerson1, academicYear, ministry, gradeClass);
        sp1.assignServant(servantPerson);
        StudentPlacement sp2 = new StudentPlacement(studentPerson2, academicYear, ministry, gradeClass);
        sp2.assignServant(servantPerson);

        when(studentPlacementRepository.findAllByAcademicYearIdAndServantIdAndStatus(1L, 10L, StudentStatus.ACTIVE))
                .thenReturn(List.of(sp1, sp2));

        // 0 visits in current week
        when(visitRecordRepository.findAllByWeekId(5L)).thenReturn(List.of());

        when(notificationRepository.existsByUserIdAndTypeAndReferenceId(100L, NotificationType.WEEKLY_REMINDER, "WEEK_5"))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        int sent = notificationService.triggerWeeklyRemindersManually();

        assertEquals(1, sent);
        verify(notificationRepository).save(argThat(n ->
                n.getUser().equals(account1) &&
                n.getType() == NotificationType.WEEKLY_REMINDER &&
                n.getReferenceId().equals("WEEK_5") &&
                n.getMessage().contains("لديك مخدومان لم يتم افتقادهما هذا الأسبوع.")
        ));
    }

    @Test
    void triggerWeeklyRemindersManually_alreadyNotified_doesNotDuplicate() {
        when(weekService.getCurrentWeekEntity()).thenReturn(currentWeek);
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);

        StaffPlacement staffPlacement = new StaffPlacement(servantPerson, academicYear, ministry, gradeClass);
        when(staffPlacementRepository.findAllActiveByAcademicYearId(1L)).thenReturn(List.of(staffPlacement));
        when(userAccountRepository.findByPersonId(10L)).thenReturn(Optional.of(account1));

        StudentPlacement sp1 = new StudentPlacement(studentPerson1, academicYear, ministry, gradeClass);
        sp1.assignServant(servantPerson);

        when(studentPlacementRepository.findAllByAcademicYearIdAndServantIdAndStatus(1L, 10L, StudentStatus.ACTIVE))
                .thenReturn(List.of(sp1));
        when(visitRecordRepository.findAllByWeekId(5L)).thenReturn(List.of());

        // Already notified!
        when(notificationRepository.existsByUserIdAndTypeAndReferenceId(100L, NotificationType.WEEKLY_REMINDER, "WEEK_5"))
                .thenReturn(true);

        int sent = notificationService.triggerWeeklyRemindersManually();

        assertEquals(0, sent);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void triggerWeeklyRemindersManually_allVisited_doesNotSend() {
        when(weekService.getCurrentWeekEntity()).thenReturn(currentWeek);
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);

        StaffPlacement staffPlacement = new StaffPlacement(servantPerson, academicYear, ministry, gradeClass);
        when(staffPlacementRepository.findAllActiveByAcademicYearId(1L)).thenReturn(List.of(staffPlacement));
        when(userAccountRepository.findByPersonId(10L)).thenReturn(Optional.of(account1));

        StudentPlacement sp1 = new StudentPlacement(studentPerson1, academicYear, ministry, gradeClass);
        sp1.assignServant(servantPerson);

        when(studentPlacementRepository.findAllByAcademicYearIdAndServantIdAndStatus(1L, 10L, StudentStatus.ACTIVE))
                .thenReturn(List.of(sp1));

        // Student is already visited!
        VisitRecord visit = new VisitRecord();
        visit.setStudent(studentPerson1);
        when(visitRecordRepository.findAllByWeekId(5L)).thenReturn(List.of(visit));

        int sent = notificationService.triggerWeeklyRemindersManually();

        assertEquals(0, sent);
        verify(notificationRepository, never()).save(any());
    }
}
