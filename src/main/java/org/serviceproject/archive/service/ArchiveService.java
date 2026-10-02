package org.serviceproject.archive.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serviceproject.archive.dto.ArchiveSummaryResponse;
import org.serviceproject.archive.dto.DeletedPersonResponse;
import org.serviceproject.attendance.repository.AttendanceSessionRepository;
import org.serviceproject.audit.repository.AuditLogRepository;
import org.serviceproject.common.config.AppProperties;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.common.util.DateUtil;
import org.serviceproject.confession.repository.ConfessionRecordRepository;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.repository.PersonRepository;
import org.serviceproject.users.repository.UserAccountRepository;
import org.serviceproject.visits.repository.VisitRecordRepository;
import org.serviceproject.weeks.dto.WeekResponse;
import org.serviceproject.weeks.entity.Week;
import org.serviceproject.weeks.repository.WeekRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service managing access to historical and archived data categories.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveService {

    private final PersonRepository personRepository;
    private final WeekRepository weekRepository;
    private final VisitRecordRepository visitRecordRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final ConfessionRecordRepository confessionRecordRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserAccountRepository userAccountRepository;
    private final AppProperties appProperties;

    /**
     * Lists all soft-deleted people with their classification.
     * General Admin only.
     */
    @Transactional(readOnly = true)
    public List<DeletedPersonResponse> getDeletedPeople(UserPrincipal principal) {
        if (principal == null || !principal.isAdmin()) {
            throw AppException.forbidden("ACCESS_DENIED", "غير مصرح لك بعرض أرشيف المحذوفين");
        }

        List<Person> deletedPersons = personRepository.findAllByDeletedAtIsNotNull();
        List<DeletedPersonResponse> responses = new ArrayList<>();

        for (Person p : deletedPersons) {
            boolean isStaff = userAccountRepository.findByPersonId(p.getId()).isPresent();
            String personType = isStaff ? "خادم" : "مخدوم";

            responses.add(new DeletedPersonResponse(
                    p.getId(),
                    p.getFullName(),
                    p.getPhone(),
                    p.getGender(),
                    personType,
                    p.getDeletedAt(),
                    "",
                    ""
            ));
        }

        return responses;
    }

    /**
     * Lists all historical weeks that are dynamically locked (older than 30 days).
     */
    @Transactional(readOnly = true)
    public List<WeekResponse> getLockedWeeks(UserPrincipal principal) {
        if (principal == null) {
            throw AppException.unauthorized("UNAUTHORIZED", "يرجى تسجيل الدخول");
        }

        List<Week> allWeeks = weekRepository.findAll();
        return allWeeks.stream()
                .filter(w -> DateUtil.isWeekLocked(w.getEndDate(), appProperties.weekLockDays(), appProperties.timeZone()))
                .map(w -> new WeekResponse(
                        w.getId(),
                        w.getStartDate(),
                        w.getEndDate(),
                        true,
                        !w.isDeleted()
                ))
                .toList();
    }

    /**
     * Returns summary metrics across archive tabs according to user's permissions.
     */
    @Transactional(readOnly = true)
    public ArchiveSummaryResponse getArchiveSummary(UserPrincipal principal) {
        if (principal == null) {
            throw AppException.unauthorized("UNAUTHORIZED", "يرجى تسجيل الدخول");
        }

        Long deletedCount = principal.isAdmin()
                ? (long) personRepository.findAllByDeletedAtIsNotNull().size()
                : null;

        Long auditCount = principal.isAdmin()
                ? auditLogRepository.count()
                : null;

        long lockedWeeksCount = weekRepository.findAll().stream()
                .filter(w -> DateUtil.isWeekLocked(w.getEndDate(), appProperties.weekLockDays(), appProperties.timeZone()))
                .count();

        long totalVisits = visitRecordRepository.count();
        long totalSessions = attendanceSessionRepository.count();
        long totalConfessions = confessionRecordRepository.count();

        return new ArchiveSummaryResponse(
                deletedCount,
                lockedWeeksCount,
                totalVisits,
                totalSessions,
                totalConfessions,
                auditCount
        );
    }
}
