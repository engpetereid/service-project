package org.serviceproject.confession.service;

import lombok.RequiredArgsConstructor;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.academic.service.AcademicYearService;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.confession.dto.ConfessionResponse;
import org.serviceproject.confession.dto.ConfessionSessionDto;
import org.serviceproject.confession.dto.CreateConfessionRequest;
import org.serviceproject.confession.dto.CreateConfessionSessionRequest;
import org.serviceproject.confession.dto.StudentConfessionSummaryDto;
import org.serviceproject.confession.dto.UpdateConfessionRequest;
import org.serviceproject.confession.entity.ConfessionRecord;
import org.serviceproject.confession.repository.ConfessionRecordRepository;
import org.serviceproject.students.entity.StudentPlacement;
import org.serviceproject.students.entity.StudentStatus;
import org.serviceproject.students.repository.StudentPlacementRepository;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.repository.PersonRepository;
import org.serviceproject.users.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service managing student confession records.
 * Linked directly to Student and AcademicYear (independent of weekly periods).
 * Enforces student viewing/editing scope boundaries.
 */
@Service
@RequiredArgsConstructor
public class ConfessionService {

    private final ConfessionRecordRepository confessionRecordRepository;
    private final StudentPlacementRepository studentPlacementRepository;
    private final PersonRepository personRepository;
    private final AcademicYearService academicYearService;
    private final UserAccountRepository userAccountRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.serviceproject.common.config.AppProperties appProperties;

    @Transactional
    public ConfessionResponse create(CreateConfessionRequest request, UserPrincipal principal) {
        AcademicYear currentYear = academicYearService.getCurrentEntity();

        StudentPlacement placement = studentPlacementRepository.findByPersonIdAndAcademicYearId(
                request.studentId(), currentYear.getId())
                .orElseThrow(() -> AppException.notFound("STUDENT_NOT_PLACED", "المخدوم غير مسكن في العام الدراسي الحالي"));

        checkScope(placement, principal);

        if (confessionRecordRepository.existsByStudentIdAndConfessionDate(request.studentId(), request.confessionDate())) {
            throw AppException.conflict("CONFESSION_EXISTS", "يوجد اعتراف مسجل بالفعل لهذا المخدوم في هذا التاريخ");
        }

        Person student = placement.getPerson();
        String father = (request.confessionFather() != null && !request.confessionFather().isBlank())
                ? request.confessionFather().trim()
                : student.getConfessionFather();

        if (student.getConfessionFather() == null && father != null && !father.isBlank()) {
            student.setConfessionFather(father);
            personRepository.save(student);
        }

        UserAccount recorder = userAccountRepository.findById(principal.getUserId())
                .orElseThrow(() -> AppException.unauthorized("RECORDER_NOT_FOUND", "المستخدم المسجل غير موجود"));

        ConfessionRecord record = new ConfessionRecord(
                student,
                currentYear,
                request.confessionDate(),
                father,
                request.notes(),
                recorder
        );

        record = confessionRecordRepository.save(record);
        return toResponse(record);
    }

    @Transactional
    public ConfessionSessionDto createSession(CreateConfessionSessionRequest request, UserPrincipal principal) {
        AcademicYear currentYear = academicYearService.getCurrentEntity();
        UserAccount recorder = userAccountRepository.findById(principal.getUserId())
                .orElseThrow(() -> AppException.unauthorized("RECORDER_NOT_FOUND", "المستخدم المسجل غير موجود"));

        String father = request.confessionFather().trim();
        LocalDate date = request.sessionDate();

        List<ConfessionRecord> savedRecords = new ArrayList<>();
        List<ConfessionSessionDto.SessionStudentDto> studentDtos = new ArrayList<>();

        for (Long studentId : request.studentIds()) {
            StudentPlacement placement = studentPlacementRepository.findByPersonIdAndAcademicYearId(
                    studentId, currentYear.getId())
                    .orElseThrow(() -> AppException.notFound("STUDENT_NOT_PLACED", "المخدوم غير مسكن في العام الدراسي الحالي: " + studentId));

            checkScope(placement, principal);

            Person student = placement.getPerson();
            student.setConfessionFather(father);
            personRepository.save(student);

            Optional<ConfessionRecord> existingOpt = confessionRecordRepository.findByStudentIdAndConfessionDate(studentId, date);
            ConfessionRecord record;
            if (existingOpt.isPresent()) {
                record = existingOpt.get();
                record.setConfessionFather(father);
                if (request.notes() != null && !request.notes().isBlank()) {
                    record.setNotes(request.notes().trim());
                }
                record.setRecordedBy(recorder);
            } else {
                record = new ConfessionRecord(
                        student,
                        currentYear,
                        date,
                        father,
                        request.notes() != null ? request.notes().trim() : null,
                        recorder
                );
            }
            record = confessionRecordRepository.save(record);
            savedRecords.add(record);

            studentDtos.add(new ConfessionSessionDto.SessionStudentDto(
                    student.getId(),
                    student.getFullName(),
                    student.getPhone(),
                    placement.getGradeClass() != null ? placement.getGradeClass().getName() : null,
                    record.getId()
            ));
        }

        String sessionId = date.toString() + "_" + (father.isBlank() ? "unknown" : father.replaceAll("\\s+", "_"));
        return new ConfessionSessionDto(
                sessionId,
                date,
                father,
                savedRecords.size(),
                request.notes() != null ? request.notes().trim() : null,
                recorder.getPerson() != null ? recorder.getPerson().getFullName() : null,
                studentDtos
        );
    }

    @Transactional(readOnly = true)
    public List<ConfessionSessionDto> getSessions(UserPrincipal principal) {
        AcademicYear currentYear = academicYearService.getCurrentEntity();
        List<StudentPlacement> placements;

        if (principal.isAdmin()) {
            placements = studentPlacementRepository.findAllByAcademicYearIdAndStatus(
                    currentYear.getId(), StudentStatus.ACTIVE);
        } else if (principal.isServiceSecretary()) {
            placements = studentPlacementRepository.findAllByAcademicYearIdAndMinistryIdAndStatus(
                    currentYear.getId(), principal.getServiceSecretaryMinistryId(), StudentStatus.ACTIVE);
        } else if (principal.isClassSecretary()) {
            placements = studentPlacementRepository.findAllByAcademicYearIdAndClassIdAndStatus(
                    currentYear.getId(), principal.getClassSecretaryClassId(), StudentStatus.ACTIVE);
        } else if (principal.isServant()) {
            placements = studentPlacementRepository.findAllByAcademicYearIdAndServantIdAndStatus(
                    currentYear.getId(), principal.getPersonId(), StudentStatus.ACTIVE);
        } else {
            throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية لعرض سجلات الاعتراف");
        }

        Map<Long, StudentPlacement> placementByStudentId = placements.stream()
                .collect(Collectors.toMap(p -> p.getPerson().getId(), p -> p, (a, b) -> a));

        List<ConfessionRecord> allRecords = confessionRecordRepository.findAllByAcademicYearId(currentYear.getId());

        List<ConfessionRecord> scopedRecords = allRecords.stream()
                .filter(cr -> placementByStudentId.containsKey(cr.getStudent().getId()))
                .toList();

        record SessionKey(LocalDate date, String father) {}

        Map<SessionKey, List<ConfessionRecord>> grouped = scopedRecords.stream()
                .collect(Collectors.groupingBy(
                        cr -> new SessionKey(
                                cr.getConfessionDate(),
                                cr.getConfessionFather() != null && !cr.getConfessionFather().isBlank()
                                        ? cr.getConfessionFather().trim()
                                        : "غير محدد"
                        ),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<ConfessionSessionDto> sessionDtos = new ArrayList<>();

        for (Map.Entry<SessionKey, List<ConfessionRecord>> entry : grouped.entrySet()) {
            SessionKey key = entry.getKey();
            List<ConfessionRecord> records = entry.getValue();

            List<ConfessionSessionDto.SessionStudentDto> studentItems = records.stream().map(cr -> {
                StudentPlacement sp = placementByStudentId.get(cr.getStudent().getId());
                return new ConfessionSessionDto.SessionStudentDto(
                        cr.getStudent().getId(),
                        cr.getStudent().getFullName(),
                        cr.getStudent().getPhone(),
                        (sp != null && sp.getGradeClass() != null) ? sp.getGradeClass().getName() : null,
                        cr.getId()
                );
            }).toList();

            String sessionNotes = records.stream()
                    .map(ConfessionRecord::getNotes)
                    .filter(n -> n != null && !n.isBlank())
                    .findFirst()
                    .orElse(null);

            String recorderName = (records.get(0).getRecordedBy() != null && records.get(0).getRecordedBy().getPerson() != null)
                    ? records.get(0).getRecordedBy().getPerson().getFullName()
                    : null;

            String sessionId = key.date().toString() + "_" + key.father().replaceAll("\\s+", "_");

            sessionDtos.add(new ConfessionSessionDto(
                    sessionId,
                    key.date(),
                    key.father(),
                    studentItems.size(),
                    sessionNotes,
                    recorderName,
                    studentItems
            ));
        }

        sessionDtos.sort((a, b) -> b.sessionDate().compareTo(a.sessionDate()));

        return sessionDtos;
    }

    @Transactional(readOnly = true)
    public List<StudentConfessionSummaryDto> getOverview(UserPrincipal principal) {
        AcademicYear currentYear = academicYearService.getCurrentEntity();
        List<StudentPlacement> placements;

        if (principal.isAdmin()) {
            placements = studentPlacementRepository.findAllByAcademicYearIdAndStatus(
                    currentYear.getId(), StudentStatus.ACTIVE);
        } else if (principal.isServiceSecretary()) {
            placements = studentPlacementRepository.findAllByAcademicYearIdAndMinistryIdAndStatus(
                    currentYear.getId(), principal.getServiceSecretaryMinistryId(), StudentStatus.ACTIVE);
        } else if (principal.isClassSecretary()) {
            placements = studentPlacementRepository.findAllByAcademicYearIdAndClassIdAndStatus(
                    currentYear.getId(), principal.getClassSecretaryClassId(), StudentStatus.ACTIVE);
        } else if (principal.isServant()) {
            placements = studentPlacementRepository.findAllByAcademicYearIdAndServantIdAndStatus(
                    currentYear.getId(), principal.getPersonId(), StudentStatus.ACTIVE);
        } else {
            throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية لعرض سجلات الاعتراف");
        }

        List<ConfessionRecord> allRecords = confessionRecordRepository.findAllByAcademicYearId(currentYear.getId());
        Map<Long, List<ConfessionRecord>> recordsByStudent = allRecords.stream()
                .collect(Collectors.groupingBy(cr -> cr.getStudent().getId()));

        LocalDate today = (appProperties != null)
                ? org.serviceproject.common.util.DateUtil.today(appProperties.timeZone())
                : LocalDate.now();
        List<StudentConfessionSummaryDto> summaries = new ArrayList<>();

        for (StudentPlacement sp : placements) {
            Person student = sp.getPerson();
            List<ConfessionRecord> studentRecords = recordsByStudent.getOrDefault(student.getId(), List.of());

            LocalDate lastDate = null;
            Long daysSince = null;
            String status = "NEVER";
            String father = student.getConfessionFather();

            if (!studentRecords.isEmpty()) {
                ConfessionRecord latest = studentRecords.get(0);
                lastDate = latest.getConfessionDate();
                daysSince = ChronoUnit.DAYS.between(lastDate, today);
                if (latest.getConfessionFather() != null && !latest.getConfessionFather().isBlank()) {
                    father = latest.getConfessionFather();
                }

                if (daysSince < 30) {
                    status = "UP_TO_DATE";
                } else if (daysSince <= 60) {
                    status = "OVERDUE";
                } else {
                    status = "CRITICAL";
                }
            }

            summaries.add(new StudentConfessionSummaryDto(
                    student.getId(),
                    student.getFullName(),
                    student.getPhone(),
                    student.getGender() != null ? student.getGender().name() : "MALE",
                    sp.getGradeClass() != null ? sp.getGradeClass().getId() : null,
                    sp.getGradeClass() != null ? sp.getGradeClass().getName() : null,
                    sp.getResponsibleServant() != null ? sp.getResponsibleServant().getId() : null,
                    sp.getResponsibleServant() != null ? sp.getResponsibleServant().getFullName() : null,
                    father,
                    lastDate,
                    daysSince,
                    studentRecords.size(),
                    status
            ));
        }

        return summaries;
    }

    @Transactional(readOnly = true)
    public List<ConfessionResponse> findByStudent(Long studentId, UserPrincipal principal) {
        AcademicYear currentYear = academicYearService.getCurrentEntity();
        StudentPlacement placement = studentPlacementRepository.findByPersonIdAndAcademicYearId(
                studentId, currentYear.getId())
                .orElseThrow(() -> AppException.notFound("STUDENT_NOT_PLACED", "المخدوم غير مسكن في العام الدراسي الحالي"));

        checkScope(placement, principal);

        return confessionRecordRepository.findAllByStudentId(studentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ConfessionResponse update(Long id, UpdateConfessionRequest request, UserPrincipal principal) {
        ConfessionRecord record = confessionRecordRepository.findByIdWithDetails(id)
                .orElseThrow(() -> AppException.notFound("CONFESSION_NOT_FOUND", "سجل الاعتراف غير موجود"));

        AcademicYear currentYear = academicYearService.getCurrentEntity();
        StudentPlacement placement = studentPlacementRepository.findByPersonIdAndAcademicYearId(
                record.getStudent().getId(), currentYear.getId())
                .orElseThrow(() -> AppException.notFound("STUDENT_NOT_PLACED", "المخدوم غير مسكن في العام الحالي"));

        checkScope(placement, principal);

        if (!record.getConfessionDate().equals(request.confessionDate())
                && confessionRecordRepository.existsByStudentIdAndConfessionDate(record.getStudent().getId(), request.confessionDate())) {
            throw AppException.conflict("CONFESSION_EXISTS", "يوجد اعتراف مسجل بالفعل لهذا المخدوم في هذا التاريخ");
        }

        record.setConfessionDate(request.confessionDate());
        record.setConfessionFather(request.confessionFather());
        record.setNotes(request.notes());

        record = confessionRecordRepository.save(record);
        return toResponse(record);
    }

    @Transactional
    public void delete(Long id, UserPrincipal principal) {
        ConfessionRecord record = confessionRecordRepository.findByIdWithDetails(id)
                .orElseThrow(() -> AppException.notFound("CONFESSION_NOT_FOUND", "سجل الاعتراف غير موجود"));

        AcademicYear currentYear = academicYearService.getCurrentEntity();
        StudentPlacement placement = studentPlacementRepository.findByPersonIdAndAcademicYearId(
                record.getStudent().getId(), currentYear.getId())
                .orElseThrow(() -> AppException.notFound("STUDENT_NOT_PLACED", "المخدوم غير مسكن في العام الحالي"));

        checkScope(placement, principal);

        confessionRecordRepository.delete(record);
    }

    // ── Internal Helpers ─────────────────────────────────────────────

    private void checkScope(StudentPlacement placement, UserPrincipal principal) {
        if (principal.isAdmin()) return;

        if (principal.isServiceSecretary()) {
            if (placement.getMinistry() != null && placement.getMinistry().getId().equals(principal.getServiceSecretaryMinistryId())) return;
            throw AppException.forbidden("ACCESS_DENIED", "لا يمكنك التعامل مع اعترافات مخدوم خارج خدمتك المصرح بها");
        }

        if (principal.isClassSecretary()) {
            if (placement.getGradeClass() != null && placement.getGradeClass().getId().equals(principal.getClassSecretaryClassId())) return;
            throw AppException.forbidden("ACCESS_DENIED", "لا يمكنك التعامل مع اعترافات مخدوم خارج فصلك المصرح به");
        }

        if (principal.isServant()) {
            if (placement.getResponsibleServant() != null && placement.getResponsibleServant().getId().equals(principal.getPersonId())) return;
            throw AppException.forbidden("ACCESS_DENIED", "يمكنك فقط التعامل مع اعترافات المخدومين المسندين إليك");
        }

        throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية للتعامل مع سجلات الاعتراف");
    }

    private ConfessionResponse toResponse(ConfessionRecord cr) {
        return new ConfessionResponse(
                cr.getId(),
                cr.getStudent().getId(),
                cr.getStudent().getFullName(),
                cr.getAcademicYear().getId(),
                cr.getAcademicYear().getName(),
                cr.getConfessionDate(),
                cr.getConfessionFather(),
                cr.getNotes(),
                cr.getRecordedBy().getId(),
                cr.getRecordedBy().getPerson().getFullName()
        );
    }
}
