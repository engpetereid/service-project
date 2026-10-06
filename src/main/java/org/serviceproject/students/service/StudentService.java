package org.serviceproject.students.service;

import lombok.RequiredArgsConstructor;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.academic.service.AcademicYearService;
import org.serviceproject.classes.entity.GradeClass;
import org.serviceproject.classes.repository.GradeClassRepository;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.ministries.entity.Ministry;
import org.serviceproject.ministries.repository.MinistryRepository;
import org.serviceproject.staff.entity.StaffPlacement;
import org.serviceproject.staff.repository.StaffPlacementRepository;
import org.serviceproject.students.dto.BatchAssignServantRequest;
import org.serviceproject.students.dto.BatchMoveClassRequest;
import org.serviceproject.students.dto.ChangeAssignmentRequest;
import org.serviceproject.students.dto.CreateStudentRequest;
import org.serviceproject.students.dto.StudentResponse;
import org.serviceproject.students.dto.UpdateStudentRequest;
import org.serviceproject.students.entity.StudentPlacement;
import org.serviceproject.students.entity.StudentStatus;
import org.serviceproject.students.repository.StudentPlacementRepository;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.repository.PersonRepository;
import org.serviceproject.users.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service managing students and their academic-year placements.
 * Enforces strict role-based scope boundaries:
 * - GENERAL_ADMIN: full access to all students, can change ministry/class.
 * - SERVICE_SECRETARY: restricted to own ministry; cannot change student ministry/class.
 * - CLASS_SECRETARY: restricted to own class; cannot change student ministry/class.
 * - SERVANT: restricted to assigned students for viewing; can add students to own class;
 *   can edit allowed fields of assigned students (cannot change service, class, or servant).
 */
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentPlacementRepository studentPlacementRepository;
    private final PersonRepository personRepository;
    private final MinistryRepository ministryRepository;
    private final GradeClassRepository gradeClassRepository;
    private final StaffPlacementRepository staffPlacementRepository;
    private final AcademicYearService academicYearService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    private final UserAccountRepository userAccountRepository;

    @Transactional(readOnly = true)
    public List<StudentResponse> findAll(UserPrincipal principal, Long ministryId, Long classId, Long servantId, String search) {
        return findAll(principal, ministryId, classId, servantId, null, search);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> findAll(UserPrincipal principal, Long ministryId, Long classId, Long servantId, String scope, String search) {
        AcademicYear currentYear = academicYearService.getCurrentEntity();
        Long yearId = currentYear.getId();

        if (search != null && !search.trim().isEmpty() && (principal.isAdmin() || principal.isServiceSecretary())) {
            List<StudentPlacement> results = studentPlacementRepository.searchByAcademicYearIdAndStatus(
                    yearId, search.trim(), StudentStatus.ACTIVE);
            return results.stream()
                    .filter(sp -> isWithinReadScope(sp, principal))
                    .filter(sp -> ministryId == null || (sp.getMinistry() != null && ministryId.equals(sp.getMinistry().getId())))
                    .filter(sp -> classId == null || (sp.getGradeClass() != null && classId.equals(sp.getGradeClass().getId())))
                    .filter(sp -> servantId == null || (sp.getResponsibleServant() != null && servantId.equals(sp.getResponsibleServant().getId())))
                    .map(this::toResponse)
                    .toList();
        }

        if (principal.isAdmin()) {
            if (servantId != null) {
                return studentPlacementRepository.findAllByAcademicYearIdAndServantIdAndStatus(yearId, servantId, StudentStatus.ACTIVE).stream()
                        .map(this::toResponse).toList();
            } else if (classId != null) {
                return studentPlacementRepository.findAllByAcademicYearIdAndClassIdAndStatus(yearId, classId, StudentStatus.ACTIVE).stream()
                        .map(this::toResponse).toList();
            } else if (ministryId != null) {
                return studentPlacementRepository.findAllByAcademicYearIdAndMinistryIdAndStatus(yearId, ministryId, StudentStatus.ACTIVE).stream()
                        .map(this::toResponse).toList();
            } else {
                return studentPlacementRepository.findAllByAcademicYearIdAndStatus(yearId, StudentStatus.ACTIVE).stream()
                        .map(this::toResponse).toList();
            }
        }

        if (principal.isServiceSecretary()) {
            Set<Long> scopedMinistryIds = principal.getServiceSecretaryMinistryIds();
            if (ministryId != null && !scopedMinistryIds.contains(ministryId)) {
                throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية على هذه الخدمة");
            }
            if (classId != null) {
                GradeClass gc = getGradeClassOrThrow(classId);
                if (!scopedMinistryIds.contains(gc.getMinistry().getId())) {
                    throw AppException.forbidden("ACCESS_DENIED", "هذا الفصل لا يتبع خدمتك المصرح بها");
                }
                return studentPlacementRepository.findAllByAcademicYearIdAndClassIdAndStatus(yearId, classId, StudentStatus.ACTIVE).stream()
                        .map(this::toResponse).toList();
            }
            if (servantId != null) {
                return studentPlacementRepository.findAllByAcademicYearIdAndServantIdAndStatus(yearId, servantId, StudentStatus.ACTIVE).stream()
                        .filter(sp -> sp.getMinistry() != null && scopedMinistryIds.contains(sp.getMinistry().getId()))
                        .map(this::toResponse).toList();
            }
            Long targetMid = ministryId != null ? ministryId : principal.getServiceSecretaryMinistryId();
            return studentPlacementRepository.findAllByAcademicYearIdAndMinistryIdAndStatus(yearId, targetMid, StudentStatus.ACTIVE).stream()
                        .map(this::toResponse).toList();
        }

        if (principal.isClassSecretary()) {
            Set<Long> scopedClassIds = principal.getClassSecretaryClassIds();
            Long scopedClassId = principal.getClassSecretaryClassId();
            if ("attendance".equalsIgnoreCase(scope) || "ministry".equalsIgnoreCase(scope)) {
                GradeClass gc = scopedClassId != null ? gradeClassRepository.findByIdWithMinistry(scopedClassId).orElse(null) : null;
                if (gc != null && gc.getMinistry() != null) {
                    List<StudentPlacement> list = studentPlacementRepository.findAllByAcademicYearIdAndMinistryIdAndStatus(
                            yearId, gc.getMinistry().getId(), StudentStatus.ACTIVE);
                    if (classId != null) {
                        list = list.stream().filter(sp -> sp.getGradeClass() != null && classId.equals(sp.getGradeClass().getId())).toList();
                    }
                    if (search != null && !search.isBlank()) {
                        String searchLower = search.trim().toLowerCase();
                        list = list.stream().filter(sp ->
                                (sp.getPerson() != null && sp.getPerson().getFullName() != null && sp.getPerson().getFullName().toLowerCase().contains(searchLower)) ||
                                (sp.getPerson() != null && sp.getPerson().getPhone() != null && sp.getPerson().getPhone().contains(searchLower)) ||
                                (sp.getGuardianPhone() != null && sp.getGuardianPhone().contains(searchLower))
                        ).toList();
                    }
                    return list.stream().map(this::toResponse).toList();
                }
            }
            if (classId != null && !scopedClassIds.contains(classId)) {
                throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية على هذا الفصل");
            }
            Long targetCid = classId != null ? classId : scopedClassId;
            List<StudentPlacement> list;
            if (servantId != null) {
                list = studentPlacementRepository.findAllByAcademicYearIdAndServantIdAndStatus(yearId, servantId, StudentStatus.ACTIVE).stream()
                        .filter(sp -> sp.getGradeClass() != null && scopedClassIds.contains(sp.getGradeClass().getId()))
                        .toList();
            } else {
                list = studentPlacementRepository.findAllByAcademicYearIdAndClassIdAndStatus(yearId, targetCid, StudentStatus.ACTIVE);
            }

            if (search != null && !search.isBlank()) {
                String searchLower = search.trim().toLowerCase();
                list = list.stream().filter(sp ->
                        (sp.getPerson() != null && sp.getPerson().getFullName() != null && sp.getPerson().getFullName().toLowerCase().contains(searchLower)) ||
                        (sp.getPerson() != null && sp.getPerson().getPhone() != null && sp.getPerson().getPhone().contains(searchLower)) ||
                        (sp.getGuardianPhone() != null && sp.getGuardianPhone().contains(searchLower))
                ).toList();
            }

            return list.stream().map(this::toResponse).toList();
        }

        if (principal.isServant()) {
            List<StudentPlacement> list;
            if ("attendance".equalsIgnoreCase(scope) || "ministry".equalsIgnoreCase(scope)) {
                StaffPlacement servantPlacement = staffPlacementRepository.findByPersonIdAndAcademicYearId(principal.getPersonId(), yearId)
                        .orElse(null);
                if (servantPlacement != null && servantPlacement.getMinistry() != null) {
                    list = studentPlacementRepository.findAllByAcademicYearIdAndMinistryIdAndStatus(
                            yearId, servantPlacement.getMinistry().getId(), StudentStatus.ACTIVE);
                } else {
                    list = List.of();
                }
            } else if ("class".equalsIgnoreCase(scope) || "all".equalsIgnoreCase(scope)) {
                StaffPlacement servantPlacement = staffPlacementRepository.findByPersonIdAndAcademicYearId(principal.getPersonId(), yearId)
                        .orElse(null);
                if (servantPlacement != null && servantPlacement.getGradeClass() != null) {
                    list = studentPlacementRepository.findAllByAcademicYearIdAndClassIdAndStatus(
                            yearId, servantPlacement.getGradeClass().getId(), StudentStatus.ACTIVE);
                } else {
                    list = List.of();
                }
            } else {
                list = studentPlacementRepository.findAllByAcademicYearIdAndServantIdAndStatus(
                        yearId, principal.getPersonId(), StudentStatus.ACTIVE);
            }

            if (classId != null) {
                list = list.stream().filter(sp -> sp.getGradeClass() != null && classId.equals(sp.getGradeClass().getId())).toList();
            }

            if (search != null && !search.isBlank()) {
                String searchLower = search.trim().toLowerCase();
                list = list.stream().filter(sp ->
                        (sp.getPerson() != null && sp.getPerson().getFullName() != null && sp.getPerson().getFullName().toLowerCase().contains(searchLower)) ||
                        (sp.getPerson() != null && sp.getPerson().getPhone() != null && sp.getPerson().getPhone().contains(searchLower)) ||
                        (sp.getGuardianPhone() != null && sp.getGuardianPhone().contains(searchLower))
                ).toList();
            }

            return list.stream().map(this::toResponse).toList();
        }

        throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية لعرض المخدومين");
    }

    @Transactional(readOnly = true)
    public StudentResponse findById(Long personId, UserPrincipal principal) {
        AcademicYear currentYear = academicYearService.getCurrentEntity();
        StudentPlacement placement = studentPlacementRepository.findByPersonIdAndAcademicYearId(personId, currentYear.getId())
                .orElseThrow(() -> AppException.notFound("STUDENT_NOT_FOUND", "المخدوم غير موجود في العام الدراسي الحالي"));

        if (!isWithinReadScope(placement, principal)) {
            throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية لعرض هذا المخدوم");
        }

        return toResponse(placement);
    }

    @Transactional
    public StudentResponse create(CreateStudentRequest request, UserPrincipal principal) {
        if (request.ministryId() == null) {
            throw AppException.badRequest("MINISTRY_REQUIRED", "يجب تحديد الخدمة التابع لها المخدوم");
        }
        if (request.classId() == null) {
            throw AppException.badRequest("CLASS_REQUIRED", "يجب تسكين المخدوم في فصل دراسي ولا يُسمح بإضافته بدون فصل");
        }

        AcademicYear currentYear = academicYearService.getCurrentEntity();

        checkAddScope(request.ministryId(), request.classId(), principal, currentYear.getId());

        Ministry ministry = ministryRepository.findById(request.ministryId())
                .orElseThrow(() -> AppException.notFound("MINISTRY_NOT_FOUND", "الخدمة غير موجودة"));
        GradeClass gradeClass = getGradeClassOrThrow(request.classId());

        if (!gradeClass.getMinistry().getId().equals(ministry.getId())) {
            throw AppException.badRequest("CLASS_MINISTRY_MISMATCH", "الفصل المحدد لا يتبع هذه الخدمة");
        }

        Person responsibleServant = null;
        if (request.servantId() != null) {
            checkCanAssignServant(principal);
            responsibleServant = getAndValidateServant(request.servantId(), currentYear.getId(), ministry.getId(), gradeClass.getId());
        }

        String rawPhone = (request.phone() != null && !request.phone().isBlank()) ? request.phone().trim() : null;
        String rawGuardianPhone = (request.guardianPhone() != null && !request.guardianPhone().isBlank()) ? request.guardianPhone().trim() : null;

        if (rawPhone == null && rawGuardianPhone == null) {
            throw AppException.badRequest("PHONE_REQUIRED", "يجب إدخال رقم هاتف المخدوم أو رقم هاتف ولي الأمر على الأقل");
        }

        Person person = null;
        if (rawPhone != null) {
            Optional<Person> existingPersonOpt = personRepository.findByPhone(rawPhone);
            if (existingPersonOpt.isPresent()) {
                Person existing = existingPersonOpt.get();
                if (existing.isDeleted()) {
                    if (principal != null && !principal.isAdmin()) {
                        throw AppException.forbidden("CANNOT_RESTORE_PERSON", "فقط مسؤول النظام يمكنه استعادة شخص محذوف");
                    }
                    existing.restore();
                } else if (studentPlacementRepository.existsByPersonIdAndAcademicYearId(existing.getId(), currentYear.getId())) {
                    throw AppException.conflict("STUDENT_ALREADY_PLACED", "المخدوم مسجل بالفعل في هذا العام الدراسي");
                }

                if (staffPlacementRepository != null && staffPlacementRepository.existsByPersonIdAndAcademicYearId(existing.getId(), currentYear.getId())) {
                    throw AppException.conflict("PERSON_IS_STAFF", "رقم الهاتف مسجل بالفعل لخادم في الخدمة");
                }
                if (userAccountRepository != null && userAccountRepository.findByPersonId(existing.getId()).isPresent()) {
                    throw AppException.conflict("PERSON_IS_USER", "رقم الهاتف مسجل بالفعل لحساب مستخدم");
                }

                existing.setFullName(request.fullName().trim());
                existing.setGender(request.gender());
                if (request.dateOfBirth() != null) existing.setDateOfBirth(request.dateOfBirth());
                if (request.address() != null) existing.setAddress(request.address());
                if (request.confessionFather() != null) existing.setConfessionFather(request.confessionFather());
                person = personRepository.save(existing);
            }
        }

        if (person == null) {
            person = new Person();
            person.setFullName(request.fullName().trim());
            person.setPhone(rawPhone);
            person.setGender(request.gender());
            person.setDateOfBirth(request.dateOfBirth());
            person.setAddress(request.address());
            person.setConfessionFather(request.confessionFather());
            person = personRepository.save(person);
        }

        final Person finalPerson = person;
        StudentPlacement placement = studentPlacementRepository
                .findAnyByPersonIdAndAcademicYearId(finalPerson.getId(), currentYear.getId())
                .orElseGet(() -> new StudentPlacement(finalPerson, currentYear, ministry, gradeClass));
        placement.setMinistry(ministry);
        placement.setGradeClass(gradeClass);
        placement.assignServant(responsibleServant);
        placement.setStatus(StudentStatus.ACTIVE);
        placement.setGuardianPhone(rawGuardianPhone);
        placement.setTalents(request.talents());
        placement.setAdditionalDetails(request.additionalDetails());

        placement = studentPlacementRepository.save(placement);
        return toResponse(placement);
    }

    @Transactional
    public StudentResponse update(Long personId, UpdateStudentRequest request, UserPrincipal principal) {
        AcademicYear currentYear = academicYearService.getCurrentEntity();
        StudentPlacement placement = studentPlacementRepository.findByPersonIdAndAcademicYearId(personId, currentYear.getId())
                .orElseThrow(() -> AppException.notFound("STUDENT_NOT_FOUND", "المخدوم غير موجود في العام الدراسي الحالي"));

        // Check changing ministry or class (only GENERAL_ADMIN can change ministry/class)
        boolean requestedMinistryChange = request.ministryId() != null &&
                (placement.getMinistry() == null || !request.ministryId().equals(placement.getMinistry().getId()));
        boolean requestedClassChange = request.classId() != null &&
                (placement.getGradeClass() == null || !request.classId().equals(placement.getGradeClass().getId()));

        if (requestedMinistryChange || requestedClassChange) {
            if (!principal.isAdmin()) {
                throw AppException.forbidden("CANNOT_CHANGE_SERVICE_OR_CLASS", "تغيير الخدمة أو الفصل متاح للأمين العام فقط");
            }
            Long newMinistryId = request.ministryId() != null ? request.ministryId() :
                    (placement.getMinistry() != null ? placement.getMinistry().getId() : null);
            Long newClassId = request.classId() != null ? request.classId() :
                    (placement.getGradeClass() != null ? placement.getGradeClass().getId() : null);

            if (newMinistryId == null) {
                throw AppException.badRequest("MINISTRY_REQUIRED", "الخدمة مطلوبة ولا يمكن ترك المخدوم بدون خدمة");
            }
            if (newClassId == null) {
                throw AppException.badRequest("CLASS_REQUIRED", "الفصل مطلوب ولا يمكن ترك المخدوم بدون فصل دراسي");
            }

            Ministry ministry = ministryRepository.findById(newMinistryId)
                    .orElseThrow(() -> AppException.notFound("MINISTRY_NOT_FOUND", "الخدمة غير موجودة"));
            GradeClass gradeClass = getGradeClassOrThrow(newClassId);

            if (!gradeClass.getMinistry().getId().equals(ministry.getId())) {
                throw AppException.badRequest("CLASS_MINISTRY_MISMATCH", "الفصل المحدد لا يتبع هذه الخدمة");
            }

            placement.setMinistry(ministry);
            placement.setGradeClass(gradeClass);
            // Clear servant assignment on class change to avoid cross-class servant assignments
            if (requestedClassChange) {
                placement.assignServant(null);
            }
        }

        checkEditScope(placement, principal);

        Person person = placement.getPerson();
        String newPhone = (request.phone() != null && !request.phone().isBlank()) ? request.phone().trim() : null;
        String newGuardianPhone = (request.guardianPhone() != null && !request.guardianPhone().isBlank()) ? request.guardianPhone().trim() : null;

        if (newPhone == null && newGuardianPhone == null) {
            throw AppException.badRequest("PHONE_REQUIRED", "يجب إدخال رقم هاتف المخدوم أو رقم هاتف ولي الأمر على الأقل");
        }

        if (newPhone != null) {
            boolean phoneChanged = person.getPhone() == null || !person.getPhone().equals(newPhone);
            if (phoneChanged && personRepository.existsByPhone(newPhone)) {
                throw AppException.conflict("PHONE_EXISTS", "رقم الهاتف مستخدم بالفعل");
            }
            person.setPhone(newPhone);
        } else {
            person.setPhone(null);
        }

        person.setFullName(request.fullName().trim());
        person.setGender(request.gender());
        person.setDateOfBirth(request.dateOfBirth());
        person.setAddress(request.address());
        person.setConfessionFather(request.confessionFather());
        personRepository.save(person);

        placement.setGuardianPhone(newGuardianPhone);
        placement.setTalents(request.talents());
        placement.setAdditionalDetails(request.additionalDetails());

        placement = studentPlacementRepository.save(placement);
        return toResponse(placement);
    }

    @Transactional
    public StudentResponse changeAssignment(Long personId, ChangeAssignmentRequest request, UserPrincipal principal) {
        checkCanAssignServant(principal);

        AcademicYear currentYear = academicYearService.getCurrentEntity();
        StudentPlacement placement = studentPlacementRepository.findByPersonIdAndAcademicYearId(personId, currentYear.getId())
                .orElseThrow(() -> AppException.notFound("STUDENT_NOT_FOUND", "المخدوم غير موجود في العام الدراسي الحالي"));

        checkAssignmentScope(placement, principal);

        Person servant = null;
        if (request.servantId() != null) {
            Long minId = placement.getMinistry() != null ? placement.getMinistry().getId() : null;
            Long clsId = placement.getGradeClass() != null ? placement.getGradeClass().getId() : null;
            servant = getAndValidateServant(request.servantId(), currentYear.getId(), minId, clsId);
        }

        placement.assignServant(servant);
        placement = studentPlacementRepository.save(placement);

        return toResponse(placement);
    }

    @Transactional
    public List<StudentResponse> batchAssignServant(BatchAssignServantRequest request, UserPrincipal principal) {
        checkCanAssignServant(principal);
        AcademicYear currentYear = academicYearService.getCurrentEntity();
        List<StudentPlacement> updated = new java.util.ArrayList<>();

        for (Long studentId : request.studentIds()) {
            StudentPlacement placement = studentPlacementRepository.findByPersonIdAndAcademicYearId(studentId, currentYear.getId())
                    .orElseThrow(() -> AppException.notFound("STUDENT_NOT_FOUND", "المخدوم غير موجود في العام الدراسي الحالي: " + studentId));

            checkAssignmentScope(placement, principal);

            Person servant = null;
            if (request.servantId() != null) {
                Long minId = placement.getMinistry() != null ? placement.getMinistry().getId() : null;
                Long clsId = placement.getGradeClass() != null ? placement.getGradeClass().getId() : null;
                servant = getAndValidateServant(request.servantId(), currentYear.getId(), minId, clsId);
            }

            placement.assignServant(servant);
            updated.add(studentPlacementRepository.save(placement));
        }

        return updated.stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<StudentResponse> batchMoveClass(BatchMoveClassRequest request, UserPrincipal principal) {
        if (!principal.isAdmin() && !principal.isServiceSecretary()) {
            throw AppException.forbidden("ACCESS_DENIED", "نقل الفصول متاح للمسؤولين وأمناء الخدمة فقط");
        }

        if (principal.isServiceSecretary()) {
            Long scopedMinistryId = principal.getServiceSecretaryMinistryId();
            if (!scopedMinistryId.equals(request.ministryId())) {
                throw AppException.forbidden("ACCESS_DENIED", "لا يمكنك نقل مخدومين خارج خدمتك المصرح بها");
            }
        }

        AcademicYear currentYear = academicYearService.getCurrentEntity();
        Ministry ministry = ministryRepository.findById(request.ministryId())
                .orElseThrow(() -> AppException.notFound("MINISTRY_NOT_FOUND", "الخدمة غير موجودة"));
        GradeClass gradeClass = getGradeClassOrThrow(request.classId());

        if (!gradeClass.getMinistry().getId().equals(ministry.getId())) {
            throw AppException.badRequest("CLASS_MINISTRY_MISMATCH", "الفصل المحدد لا يتبع هذه الخدمة");
        }

        List<StudentPlacement> updated = new java.util.ArrayList<>();
        for (Long studentId : request.studentIds()) {
            StudentPlacement placement = studentPlacementRepository.findByPersonIdAndAcademicYearId(studentId, currentYear.getId())
                    .orElseThrow(() -> AppException.notFound("STUDENT_NOT_FOUND", "المخدوم غير موجود: " + studentId));

            if (principal.isServiceSecretary() && !placement.getMinistry().getId().equals(principal.getServiceSecretaryMinistryId())) {
                throw AppException.forbidden("ACCESS_DENIED", "لا يمكنك نقل مخدوم خارج خدمتك المصرح بها");
            }

            placement.setMinistry(ministry);
            placement.setGradeClass(gradeClass);
            // Clear servant assignment because servant belongs to the old class
            placement.assignServant(null);

            updated.add(studentPlacementRepository.save(placement));
        }

        return updated.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void softDelete(Long personId, UserPrincipal principal) {
        if (principal.isServant() && !principal.isAdmin() && !principal.isServiceSecretary() && !principal.isClassSecretary()) {
            throw AppException.forbidden("ACCESS_DENIED", "الخادم ليس لديه صلاحية حذف المخدومين");
        }

        AcademicYear currentYear = academicYearService.getCurrentEntity();
        StudentPlacement placement = studentPlacementRepository.findByPersonIdAndAcademicYearId(personId, currentYear.getId())
                .orElseThrow(() -> AppException.notFound("STUDENT_NOT_FOUND", "المخدوم غير موجود في العام الدراسي الحالي"));

        checkEditScope(placement, principal);

        Person person = placement.getPerson();
        person.softDelete();
        personRepository.save(person);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(org.serviceproject.audit.event.AuditEvent.of(
                    principal,
                    org.serviceproject.audit.entity.AuditAction.DELETE,
                    "Student",
                    personId,
                    person.getFullName(),
                    "soft_deleted"
            ));
        }
    }

    @Transactional
    public void restore(Long personId) {
        restore(personId, null);
    }

    @Transactional
    public void restore(Long personId, UserPrincipal principal) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> AppException.notFound("PERSON_NOT_FOUND", "المخدوم غير موجود"));

        if (!person.isDeleted()) {
            throw AppException.badRequest("NOT_DELETED", "هذا المخدوم غير محذوف");
        }

        person.restore();
        personRepository.save(person);

        if (userAccountRepository != null) {
            userAccountRepository.findByPersonId(person.getId()).ifPresent(acc -> {
                acc.setEnabled(true);
                userAccountRepository.save(acc);
            });
        }

        if (eventPublisher != null) {
            eventPublisher.publishEvent(org.serviceproject.audit.event.AuditEvent.of(
                    principal,
                    org.serviceproject.audit.entity.AuditAction.RESTORE,
                    "Student",
                    personId,
                    "deleted",
                    "restored: " + person.getFullName()
            ));
        }
    }

    // ── Internal Helpers ─────────────────────────────────────────────

    private boolean isWithinReadScope(StudentPlacement sp, UserPrincipal principal) {
        if (principal.isAdmin()) return true;

        if (principal.isServiceSecretary()) {
            return sp.getMinistry() != null && sp.getMinistry().getId().equals(principal.getServiceSecretaryMinistryId());
        }

        if (principal.isClassSecretary()) {
            return sp.getGradeClass() != null && sp.getGradeClass().getId().equals(principal.getClassSecretaryClassId());
        }

        if (principal.isServant()) {
            if (sp.getResponsibleServant() != null && sp.getResponsibleServant().getId().equals(principal.getPersonId())) {
                return true;
            }
            if (sp.getMinistry() != null && sp.getAcademicYear() != null) {
                return staffPlacementRepository.findByPersonIdAndAcademicYearId(principal.getPersonId(), sp.getAcademicYear().getId())
                        .map(staff -> staff.getMinistry() != null && staff.getMinistry().getId().equals(sp.getMinistry().getId()))
                        .orElse(false);
            }
            return false;
        }

        return false;
    }

    private void checkAddScope(Long targetMinistryId, Long targetClassId, UserPrincipal principal, Long currentYearId) {
        if (targetMinistryId == null || targetClassId == null) {
            throw AppException.badRequest("CLASS_REQUIRED", "يجب تحديد الخدمة والفصل الدراسي. لا يُسمح بإضافة مخدوم غير مسكن بفصل");
        }
        if (principal.isAdmin()) return;

        if (principal.isServiceSecretary()) {
            if (!targetMinistryId.equals(principal.getServiceSecretaryMinistryId())) {
                throw AppException.forbidden("ACCESS_DENIED", "لا يمكنك إضافة مخدوم خارج خدمتك المصرح بها");
            }
            return;
        }

        if (principal.isClassSecretary()) {
            if (!targetClassId.equals(principal.getClassSecretaryClassId())) {
                throw AppException.forbidden("ACCESS_DENIED", "لا يمكنك إضافة مخدوم خارج فصلك المصرح به");
            }
            return;
        }

        if (principal.isServant()) {
            // Servant can add students to their OWN class
            StaffPlacement servantPlacement = staffPlacementRepository.findByPersonIdAndAcademicYearId(principal.getPersonId(), currentYearId)
                    .orElseThrow(() -> AppException.forbidden("ACCESS_DENIED", "الخادم ليس لديه فصل محدد في العام الدراسي الحالي"));

            if (!servantPlacement.getGradeClass().getId().equals(targetClassId) ||
                !servantPlacement.getMinistry().getId().equals(targetMinistryId)) {
                throw AppException.forbidden("ACCESS_DENIED", "يمكنك فقط إضافة مخدوم في فصلك المصرح به");
            }
            return;
        }

        throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية لإضافة مخدومين");
    }

    private void checkEditScope(StudentPlacement placement, UserPrincipal principal) {
        if (principal.isAdmin()) return;

        if (principal.isServiceSecretary()) {
            if (!placement.getMinistry().getId().equals(principal.getServiceSecretaryMinistryId())) {
                throw AppException.forbidden("ACCESS_DENIED", "لا يمكنك تعديل مخدوم خارج خدمتك المصرح بها");
            }
            return;
        }

        if (principal.isClassSecretary()) {
            if (!placement.getGradeClass().getId().equals(principal.getClassSecretaryClassId())) {
                throw AppException.forbidden("ACCESS_DENIED", "لا يمكنك تعديل مخدوم خارج فصلك المصرح به");
            }
            return;
        }

        if (principal.isServant()) {
            if (placement.getResponsibleServant() != null && placement.getResponsibleServant().getId().equals(principal.getPersonId())) {
                return;
            }
            throw AppException.forbidden("ACCESS_DENIED", "يمكنك فقط تعديل بيانات المخدومين المسندين إليك");
        }

        throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية لتعديل هذا المخدوم");
    }

    private void checkCanAssignServant(UserPrincipal principal) {
        if (principal.isServant() && !principal.isAdmin() && !principal.isServiceSecretary() && !principal.isClassSecretary()) {
            throw AppException.forbidden("ACCESS_DENIED", "الخادم ليس لديه صلاحية تعيين الخادم المسؤول");
        }
    }

    private void checkAssignmentScope(StudentPlacement placement, UserPrincipal principal) {
        if (principal.isAdmin()) return;

        if (principal.isServiceSecretary()) {
            Long scopedMinistryId = principal.getServiceSecretaryMinistryId();
            if (scopedMinistryId == null || placement.getMinistry() == null ||
                !placement.getMinistry().getId().equals(scopedMinistryId)) {
                throw AppException.forbidden("ACCESS_DENIED", "لا يمكنك تغيير الخادم المسؤول لمخدوم خارج خدمتك");
            }
            return;
        }

        if (principal.isClassSecretary()) {
            Long scopedClassId = principal.getClassSecretaryClassId();
            if (scopedClassId == null || placement.getGradeClass() == null ||
                !placement.getGradeClass().getId().equals(scopedClassId)) {
                throw AppException.forbidden("ACCESS_DENIED", "لا يمكنك تغيير الخادم المسؤول لمخدوم خارج فصلك");
            }
            return;
        }

        throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية لتغيير الخادم المسؤول");
    }

    private Person getAndValidateServant(Long servantPersonId, Long academicYearId, Long expectedMinistryId, Long expectedClassId) {
        Person servant = personRepository.findByIdAndDeletedAtIsNull(servantPersonId)
                .orElseThrow(() -> AppException.notFound("SERVANT_NOT_FOUND", "الخادم غير موجود"));

        if (expectedMinistryId == null || expectedClassId == null) {
            throw AppException.badRequest("STUDENT_NOT_IN_CLASS", "يجب تسكين المخدوم في خدمة وفصل أولاً قبل تعيين خادم مسؤول له");
        }

        StaffPlacement staffPlacement = staffPlacementRepository.findByPersonIdAndAcademicYearId(servantPersonId, academicYearId)
                .orElse(null);

        boolean isValid = staffPlacement != null &&
                staffPlacement.getMinistry() != null &&
                staffPlacement.getGradeClass() != null &&
                staffPlacement.getMinistry().getId().equals(expectedMinistryId) &&
                staffPlacement.getGradeClass().getId().equals(expectedClassId);

        if (!isValid) {
            boolean isSecretaryOfClass = false;
            if (userAccountRepository != null) {
                List<UserAccount> secretaries = userAccountRepository.findClassSecretariesByClassId(expectedClassId);
                isSecretaryOfClass = secretaries.stream()
                        .anyMatch(ua -> ua.getPerson() != null && ua.getPerson().getId().equals(servantPersonId));
            }
            if (isSecretaryOfClass) {
                GradeClass gc = getGradeClassOrThrow(expectedClassId);
                AcademicYear year = academicYearService.getCurrentEntity();
                if (staffPlacement == null) {
                    staffPlacement = new StaffPlacement(servant, year, gc.getMinistry(), gc);
                } else {
                    staffPlacement.setMinistry(gc.getMinistry());
                    staffPlacement.setGradeClass(gc);
                }
                staffPlacementRepository.save(staffPlacement);
            } else {
                if (staffPlacement == null) {
                    throw AppException.badRequest("SERVANT_NOT_PLACED", "الخادم غير مسكن في هذا العام الدراسي");
                }
                throw AppException.badRequest("SERVANT_CLASS_MISMATCH", "الخادم المسؤول يجب أن يكون مسكناً في نفس الخدمة والفصل");
            }
        }

        return servant;
    }

    private GradeClass getGradeClassOrThrow(Long classId) {
        return gradeClassRepository.findByIdWithMinistry(classId)
                .orElseThrow(() -> AppException.notFound("CLASS_NOT_FOUND", "الفصل غير موجود"));
    }

    private StudentResponse toResponse(StudentPlacement sp) {
        Person p = sp.getPerson();
        Person s = sp.getResponsibleServant();

        return new StudentResponse(
                p.getId(),
                p.getFullName(),
                p.getPhone(),
                p.getGender().name(),
                p.getDateOfBirth(),
                p.getAddress(),
                p.getConfessionFather(),
                sp.getMinistry() != null ? sp.getMinistry().getId() : null,
                sp.getMinistry() != null ? sp.getMinistry().getName() : null,
                sp.getGradeClass() != null ? sp.getGradeClass().getId() : null,
                sp.getGradeClass() != null ? sp.getGradeClass().getName() : null,
                s != null ? s.getId() : null,
                s != null ? s.getFullName() : null,
                sp.getAcademicYear().getId(),
                sp.getAcademicYear().getName(),
                sp.getStatus().name(),
                sp.getGuardianPhone(),
                sp.getTalents(),
                sp.getAdditionalDetails(),
                sp.getAssignedAt(),
                !p.isDeleted()
        );
    }
}
