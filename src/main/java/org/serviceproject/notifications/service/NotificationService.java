package org.serviceproject.notifications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.academic.service.AcademicYearService;
import org.serviceproject.common.config.AppProperties;
import org.serviceproject.common.dto.PageResponse;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.common.util.DateUtil;
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
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.repository.UserAccountRepository;
import org.serviceproject.visits.entity.VisitRecord;
import org.serviceproject.visits.repository.VisitRecordRepository;
import org.serviceproject.weeks.entity.Week;
import org.serviceproject.weeks.service.WeekService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service managing in-app notifications and automated weekly visitation reminders.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final WeekService weekService;
    private final AcademicYearService academicYearService;
    private final StaffPlacementRepository staffPlacementRepository;
    private final StudentPlacementRepository studentPlacementRepository;
    private final VisitRecordRepository visitRecordRepository;
    private final UserAccountRepository userAccountRepository;
    private final AppProperties appProperties;

    /**
     * Retrieves paginated notifications for the authenticated user.
     */
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getUserNotifications(UserPrincipal principal, boolean unreadOnly, int page, int size) {
        if (principal == null) {
            throw AppException.unauthorized("UNAUTHORIZED", "يرجى تسجيل الدخول");
        }

        int pageNumber = Math.max(0, page);
        int pageSize = Math.min(100, Math.max(1, size));
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<Notification> notificationPage = unreadOnly
                ? notificationRepository.findAllByUserIdAndReadFalseOrderByCreatedAtDesc(principal.getUserId(), pageable)
                : notificationRepository.findAllByUserIdOrderByCreatedAtDesc(principal.getUserId(), pageable);

        return PageResponse.of(notificationPage.map(notificationMapper::toResponse));
    }

    /**
     * Gets the count of unread notifications for the user's notification bell.
     */
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UserPrincipal principal) {
        if (principal == null) {
            throw AppException.unauthorized("UNAUTHORIZED", "يرجى تسجيل الدخول");
        }

        long count = notificationRepository.countByUserIdAndReadFalse(principal.getUserId());
        return new UnreadCountResponse(count);
    }

    /**
     * Marks a specific notification as read.
     * Enforces that the notification belongs to the calling user.
     */
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, UserPrincipal principal) {
        if (principal == null) {
            throw AppException.unauthorized("UNAUTHORIZED", "يرجى تسجيل الدخول");
        }

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> AppException.notFound("NOTIFICATION_NOT_FOUND", "الإشعار غير موجود"));

        if (!notification.getUser().getId().equals(principal.getUserId())) {
            throw AppException.forbidden("ACCESS_DENIED", "غير مصرح لك بتعديل هذا الإشعار");
        }

        if (!notification.isRead()) {
            notification.markAsRead();
            notification = notificationRepository.save(notification);
        }

        return notificationMapper.toResponse(notification);
    }

    /**
     * Marks all unread notifications for the calling user as read.
     */
    @Transactional
    public int markAllAsRead(UserPrincipal principal) {
        if (principal == null) {
            throw AppException.unauthorized("UNAUTHORIZED", "يرجى تسجيل الدخول");
        }

        return notificationRepository.markAllAsReadByUserId(principal.getUserId(), LocalDateTime.now());
    }

    /**
     * Creates and saves an in-app notification if not already sent (deduplication via referenceId).
     */
    @Transactional
    public Notification createNotification(UserAccount user, String title, String message, NotificationType type, String referenceId) {
        if (user == null) {
            return null;
        }

        if (referenceId != null && notificationRepository.existsByUserIdAndTypeAndReferenceId(user.getId(), type, referenceId)) {
            log.debug("Notification already exists for user {} and referenceId {}", user.getId(), referenceId);
            return null;
        }

        Notification notification = new Notification(user, title, message, type, referenceId);
        return notificationRepository.save(notification);
    }

    /**
     * Scheduled job entrypoint: sends weekly reminders to servants with unvisited students
     * if the current time is within the final reminder window of the week.
     */
    @Transactional
    public int sendWeeklyReminders() {
        Week currentWeek = weekService.getCurrentWeekEntity();
        if (currentWeek == null) {
            log.debug("No current week entity found, skipping weekly reminders");
            return 0;
        }

        LocalDateTime now = DateUtil.now(appProperties.timeZone());
        LocalDateTime weekEnd = currentWeek.getEndDate().atTime(23, 59, 59);
        LocalDateTime reminderStart = weekEnd.minusHours(appProperties.weeklyReminderHours());

        if (now.isBefore(reminderStart) || now.isAfter(weekEnd)) {
            log.debug("Current time {} is outside the weekly reminder window ({} to {})", now, reminderStart, weekEnd);
            return 0;
        }

        return processWeeklyRemindersForWeek(currentWeek);
    }

    /**
     * Manual trigger for weekly reminders (bypasses window check, e.g. for admin trigger).
     */
    @Transactional
    public int triggerWeeklyRemindersManually() {
        Week currentWeek = weekService.getCurrentWeekEntity();
        if (currentWeek == null) {
            throw AppException.notFound("WEEK_NOT_FOUND", "الأسبوع الحالي غير موجود");
        }

        return processWeeklyRemindersForWeek(currentWeek);
    }

    // ── Internal Helper ──────────────────────────────────────────────

    private int processWeeklyRemindersForWeek(Week week) {
        AcademicYear currentYear = academicYearService.getCurrentEntity();
        if (currentYear == null) {
            log.warn("No current academic year found, cannot process weekly reminders");
            return 0;
        }

        List<StaffPlacement> staffPlacements = staffPlacementRepository.findAllActiveByAcademicYearId(currentYear.getId());
        if (staffPlacements.isEmpty()) {
            return 0;
        }

        List<VisitRecord> weekVisits = visitRecordRepository.findAllByWeekId(week.getId());
        Set<Long> visitedStudentIds = weekVisits.stream()
                .map(vr -> vr.getStudent().getId())
                .collect(Collectors.toSet());

        Set<Long> processedServantPersonIds = new HashSet<>();
        int sentCount = 0;

        for (StaffPlacement sp : staffPlacements) {
            Long servantPersonId = sp.getPerson().getId();
            if (!processedServantPersonIds.add(servantPersonId)) {
                continue;
            }

            Optional<UserAccount> userOpt = userAccountRepository.findByPersonId(servantPersonId);
            if (userOpt.isEmpty() || !userOpt.get().isEnabled()) {
                continue;
            }
            UserAccount account = userOpt.get();

            List<StudentPlacement> assignedStudents = studentPlacementRepository
                    .findAllByAcademicYearIdAndServantIdAndStatus(currentYear.getId(), servantPersonId, StudentStatus.ACTIVE);
            if (assignedStudents.isEmpty()) {
                continue;
            }

            long unvisitedCount = assignedStudents.stream()
                    .filter(studentPlacement -> !visitedStudentIds.contains(studentPlacement.getPerson().getId()))
                    .count();

            if (unvisitedCount > 0) {
                String referenceId = "WEEK_" + week.getId();
                if (!notificationRepository.existsByUserIdAndTypeAndReferenceId(account.getId(), NotificationType.WEEKLY_REMINDER, referenceId)) {
                    String message = formatWeeklyReminderMessage(unvisitedCount);
                    Notification notification = new Notification(
                            account,
                            "تذكير بالافتقاد الأسبوعي",
                            message,
                            NotificationType.WEEKLY_REMINDER,
                            referenceId
                    );
                    notificationRepository.save(notification);
                    sentCount++;
                    log.info("Sent weekly reminder to user {} for {} unvisited students (week {})",
                            account.getId(), unvisitedCount, week.getId());
                }
            }
        }

        return sentCount;
    }

    private String formatWeeklyReminderMessage(long count) {
        if (count == 1) {
            return "لديك مخدوم واحد لم يتم افتقاده هذا الأسبوع.";
        } else if (count == 2) {
            return "لديك مخدومان لم يتم افتقادهما هذا الأسبوع.";
        } else if (count >= 3 && count <= 10) {
            return "لديك " + count + " مخدومين لم يتم افتقادهم هذا الأسبوع.";
        } else {
            return "لديك " + count + " مخدوماً لم يتم افتقادهم هذا الأسبوع.";
        }
    }
}
