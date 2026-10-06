package org.serviceproject.staff.service;

import lombok.RequiredArgsConstructor;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.academic.service.AcademicYearService;
import org.serviceproject.classes.entity.GradeClass;
import org.serviceproject.classes.repository.GradeClassRepository;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.ministries.entity.Ministry;
import org.serviceproject.ministries.repository.MinistryRepository;
import org.serviceproject.staff.dto.CreateServantRequest;
import org.serviceproject.staff.dto.ServantResponse;
import org.serviceproject.staff.dto.UpdateServantRequest;
import org.serviceproject.staff.entity.StaffPlacement;
import org.serviceproject.staff.repository.StaffPlacementRepository;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.Role;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.entity.UserRole;
import org.serviceproject.users.repository.PersonRepository;
import org.serviceproject.users.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service managing servants and their staff placements.
 * Enforces role-based scope restrictions:
 * - GENERAL_ADMIN: full access to all servants across all ministries/classes.
 * - SERVICE_SECRETARY: restricted to servants within their scoped ministry.
 * - CLASS_SECRETARY: restricted to servants within their scoped class.
 * - SERVANT: restricted to viewing their own servant profile.
 */
@Service
@RequiredArgsConstructor
public class ServantService {

    private final StaffPlacementRepository staffPlacementRepository;
    private final PersonRepository personRepository;
    private final UserAccountRepository userAccountRepository;
    private final MinistryRepository ministryRepository;
    private final GradeClassRepository gradeClassRepository;
    private final AcademicYearService academicYearService;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    private final org.serviceproject.students.repository.StudentPlacementRepository studentPlacementRepository;
    private final org.serviceproject.notifications.service.NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<ServantResponse> findAll(UserPrincipal principal, Long ministryId, Long classId) {
        AcademicYear currentYear = academicYearService.getCurrentEntity();
        Long yearId = currentYear.getId();

        if (principal.isAdmin()) {
            if (classId != null) {
                return getServantsWithSecretaryForClass(yearId, classId, currentYear).stream()
                        .map(this::toResponse).toList();
            } else if (ministryId != null) {
                return staffPlacementRepository.findAllActiveByAcademicYearIdAndMinistryId(yearId, ministryId).stream()
                        .map(this::toResponse).toList();
            } else {
                return staffPlacementRepository.findAllActiveByAcademicYearId(yearId).stream()
                        .map(this::toResponse).toList();
            }
        }

        if (principal.isServiceSecretary()) {
            Long scopedMinistryId = principal.getServiceSecretaryMinistryId();
            if (ministryId != null && !scopedMinistryId.equals(ministryId)) {
                throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية على هذه الخدمة");
            }
            if (classId != null) {
                GradeClass gc = getGradeClassOrThrow(classId);
                if (!scopedMinistryId.equals(gc.getMinistry().getId())) {
                    throw AppException.forbidden("ACCESS_DENIED", "هذا الفصل لا يتبع خدمتك المصرح بها");
                }
                return getServantsWithSecretaryForClass(yearId, classId, currentYear).stream()
                        .map(this::toResponse).toList();
            }
            return staffPlacementRepository.findAllActiveByAcademicYearIdAndMinistryId(yearId, scopedMinistryId).stream()
                    .map(this::toResponse).toList();
        }

        if (principal.isClassSecretary()) {
            Long scopedClassId = principal.getClassSecretaryClassId();
            if (classId != null && !scopedClassId.equals(classId)) {
                throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية على هذا الفصل");
            }
            return getServantsWithSecretaryForClass(yearId, scopedClassId, currentYear).stream()
                    .map(this::toResponse).toList();
        }

        if (principal.isServant()) {
            return staffPlacementRepository.findByPersonIdAndAcademicYearId(principal.getPersonId(), yearId)
                    .map(this::toResponse)
                    .map(List::of)
                    .orElseGet(Collections::emptyList);
        }

        throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية لعرض الخدام");
    }

    @Transactional(readOnly = true)
    public ServantResponse findById(Long personId, UserPrincipal principal) {
        AcademicYear currentYear = academicYearService.getCurrentEntity();
        StaffPlacement placement = staffPlacementRepository.findByPersonIdAndAcademicYearId(personId, currentYear.getId())
                .orElseThrow(() -> AppException.notFound("SERVANT_NOT_FOUND", "الخادم غير موجود في العام الدراسي الحالي"));

        checkReadScope(placement, principal);
        return toResponse(placement);
    }

    @Transactional
    public ServantResponse create(CreateServantRequest request, UserPrincipal principal) {
        checkCanAddOrEdit(principal);
        checkTargetScope(request.ministryId(), request.classId(), principal);

        Ministry ministry = ministryRepository.findById(request.ministryId())
                .orElseThrow(() -> AppException.notFound("MINISTRY_NOT_FOUND", "الخدمة غير موجودة"));
        GradeClass gradeClass = getGradeClassOrThrow(request.classId());

        if (!gradeClass.getMinistry().getId().equals(ministry.getId())) {
            throw AppException.badRequest("CLASS_MINISTRY_MISMATCH", "الفصل المحدد لا يتبع هذه الخدمة");
        }

        AcademicYear currentYear = academicYearService.getCurrentEntity();
        String phone = request.phone().trim();

        Person person;
        Optional<Person> existingPersonOpt = personRepository.findByPhone(phone);

        if (existingPersonOpt.isPresent()) {
            person = existingPersonOpt.get();
            if (person.isDeleted()) {
                if (principal != null && !principal.isAdmin()) {
                    throw AppException.forbidden("CANNOT_RESTORE_PERSON", "فقط مسؤول النظام يمكنه استعادة شخص محذوف");
                }
                person.restore();
            } else if (staffPlacementRepository.existsByPersonIdAndAcademicYearId(person.getId(), currentYear.getId())) {
                throw AppException.conflict("SERVANT_ALREADY_PLACED", "الخادم مسجل بالفعل في هذا العام الدراسي");
            }

            Optional<UserAccount> existingAcc = userAccountRepository != null
                    ? userAccountRepository.findByPersonId(person.getId()) : Optional.empty();
            if (existingAcc.isPresent() && principal != null && !principal.isAdmin()) {
                UserAccount acc = existingAcc.get();
                if (acc.hasRole(Role.GENERAL_ADMIN) || acc.hasRole(Role.SERVICE_SECRETARY)) {
                    throw AppException.forbidden("CANNOT_MODIFY_ADMIN_ACCOUNT", "لا يمكن تعديل أو إضافة خادم مرتبط بحساب مسؤول أو أمين خدمة");
                }
            }

            person.setFullName(request.fullName().trim());
            person.setGender(request.gender());
            person.setDateOfBirth(request.dateOfBirth());
            person.setAddress(request.address());
            person.setConfessionFather(request.confessionFather());
            person = personRepository.save(person);
        } else {
            person = new Person();
            person.setFullName(request.fullName().trim());
            person.setPhone(phone);
            person.setGender(request.gender());
            person.setDateOfBirth(request.dateOfBirth());
            person.setAddress(request.address());
            person.setConfessionFather(request.confessionFather());
            person = personRepository.save(person);
        }

        final Person finalPerson = person;
        StaffPlacement placement = staffPlacementRepository
                .findAnyByPersonIdAndAcademicYearId(finalPerson.getId(), currentYear.getId())
                .orElseGet(() -> new StaffPlacement(finalPerson, currentYear, ministry, gradeClass));
        placement.setMinistry(ministry);
        placement.setGradeClass(gradeClass);
        placement = staffPlacementRepository.save(placement);

        // Always ensure user account exists with SERVANT role and enabled=true immediately
        Optional<UserAccount> existingAcc = userAccountRepository != null
                ? userAccountRepository.findByPersonId(person.getId()) : Optional.empty();
        if (existingAcc.isPresent()) {
            UserAccount acc = existingAcc.get();
            acc.setEnabled(true);
            if (!acc.hasRole(Role.SERVANT)) {
                acc.addRole(new UserRole(Role.SERVANT));
            }
            // CRITICAL FIX: NEVER overwrite an existing user's password during servant registration.
            userAccountRepository.save(acc);
        } else {
            String initialPassword = (request.password() != null && !request.password().isBlank())
                    ? request.password().trim()
                    : "Pass@" + (100000 + new java.security.SecureRandom().nextInt(900000));
            ensureServantUserAccount(person, initialPassword);
        }

        return toResponse(placement);
    }

    @Transactional
    public ServantResponse update(Long personId, UpdateServantRequest request, UserPrincipal principal) {
        checkCanAddOrEdit(principal);

        AcademicYear currentYear = academicYearService.getCurrentEntity();
        StaffPlacement placement = staffPlacementRepository.findByPersonIdAndAcademicYearId(personId, currentYear.getId())
                .orElseThrow(() -> AppException.notFound("SERVANT_NOT_FOUND", "الخادم غير موجود في العام الدراسي الحالي"));

        // Check scope on current placement
        checkEditScope(placement, principal);
        // Check scope on new target placement
        checkTargetScope(request.ministryId(), request.classId(), principal);

        Ministry ministry = ministryRepository.findById(request.ministryId())
                .orElseThrow(() -> AppException.notFound("MINISTRY_NOT_FOUND", "الخدمة غير موجودة"));
        GradeClass gradeClass = getGradeClassOrThrow(request.classId());

        if (!gradeClass.getMinistry().getId().equals(ministry.getId())) {
            throw AppException.badRequest("CLASS_MINISTRY_MISMATCH", "الفصل المحدد لا يتبع هذه الخدمة");
        }

        Person person = placement.getPerson();
        String newPhone = request.phone().trim();
        boolean phoneChanged = !person.getPhone().equals(newPhone);

        if (phoneChanged && personRepository.existsByPhone(newPhone)) {
            throw AppException.conflict("PHONE_EXISTS", "رقم الهاتف مستخدم بالفعل");
        }

        person.setFullName(request.fullName().trim());
        person.setPhone(newPhone);
        person.setGender(request.gender());
        person.setDateOfBirth(request.dateOfBirth());
        person.setAddress(request.address());
        person.setConfessionFather(request.confessionFather());
        personRepository.save(person);

        placement.setMinistry(ministry);
        placement.setGradeClass(gradeClass);
        placement = staffPlacementRepository.save(placement);

        if (phoneChanged) {
            userAccountRepository.findByPersonId(person.getId()).ifPresent(acc -> {
                acc.incrementTokenVersion();
                userAccountRepository.save(acc);
            });
        }

        return toResponse(placement);
    }

    @Transactional
    public void softDelete(Long personId, UserPrincipal principal) {
        checkCanAddOrEdit(principal);

        if (principal != null && principal.getPersonId() != null && principal.getPersonId().equals(personId)) {
            throw AppException.badRequest("CANNOT_DELETE_SELF", "لا يمكنك حذف حسابك الشخصي");
        }

        AcademicYear currentYear = academicYearService.getCurrentEntity();
        StaffPlacement placement = staffPlacementRepository.findByPersonIdAndAcademicYearId(personId, currentYear.getId())
                .orElseThrow(() -> AppException.notFound("SERVANT_NOT_FOUND", "الخادم غير موجود في العام الدراسي الحالي"));

        checkEditScope(placement, principal);

        Person person = placement.getPerson();

        if (userAccountRepository != null) {
            userAccountRepository.findByPersonId(person.getId()).ifPresent(acc -> {
                if (acc.hasRole(Role.GENERAL_ADMIN)) {
                    long adminCount = userAccountRepository.countActiveGeneralAdmins();
                    if (adminCount <= 1) {
                        throw AppException.badRequest("CANNOT_DELETE_LAST_ADMIN", "لا يمكن حذف آخر مسؤول عام (أدمن) في النظام");
                    }
                }
                if ((acc.hasRole(Role.GENERAL_ADMIN) || acc.hasRole(Role.SERVICE_SECRETARY)) && principal != null && !principal.isAdmin()) {
                    throw AppException.forbidden("CANNOT_DELETE_HIGHER_ROLE", "لا يمكنك حذف شخص لديه رتبة إدارية أعلى");
                }
                acc.setEnabled(false);
                acc.incrementTokenVersion();
                userAccountRepository.save(acc);
            });
        }

        person.softDelete();
        personRepository.save(person);

        // Automatically unassign active students assigned to this servant in current academic year
        int unassignedCount = 0;
        if (studentPlacementRepository != null) {
            List<org.serviceproject.students.entity.StudentPlacement> assignedStudents =
                    studentPlacementRepository.findAllByAcademicYearIdAndServantIdAndStatus(
                            currentYear.getId(), personId, org.serviceproject.students.entity.StudentStatus.ACTIVE);
            unassignedCount = assignedStudents.size();
            for (org.serviceproject.students.entity.StudentPlacement sp : assignedStudents) {
                sp.assignServant(null);
                studentPlacementRepository.save(sp);
            }

            // Send notification to class secretaries
            if (unassignedCount > 0 && placement.getGradeClass() != null && userAccountRepository != null && notificationService != null) {
                List<UserAccount> classSecretaries = userAccountRepository.findClassSecretariesByClassId(placement.getGradeClass().getId());
                String msg = "تم إلغاء تفعيل الخادم (" + person.getFullName() + ")، وعدد " + unassignedCount + " مخدوم أصبحوا بدون خادم مسؤول. يرجى إعادة توزيعهم.";
                for (UserAccount secAcc : classSecretaries) {
                    notificationService.createNotification(
                            secAcc,
                            "تنبيه: مخدومون بحاجة لإعادة توزيع",
                            msg,
                            org.serviceproject.notifications.entity.NotificationType.SYSTEM,
                            "SERVANT_DELETED_" + personId + "_" + System.currentTimeMillis()
                    );
                }
            }
        }

        if (eventPublisher != null) {
            eventPublisher.publishEvent(org.serviceproject.audit.event.AuditEvent.of(
                    principal,
                    org.serviceproject.audit.entity.AuditAction.DELETE,
                    "Servant",
                    personId,
                    person.getFullName(),
                    "soft_deleted (unassigned " + unassignedCount + " students)"
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
                .orElseThrow(() -> AppException.notFound("PERSON_NOT_FOUND", "الخادم غير موجود"));

        if (!person.isDeleted()) {
            throw AppException.badRequest("NOT_DELETED", "هذا الخادم غير محذوف");
        }

        person.restore();
        personRepository.save(person);

        userAccountRepository.findByPersonId(person.getId()).ifPresent(acc -> {
            acc.setEnabled(true);
            userAccountRepository.save(acc);
        });

        if (eventPublisher != null) {
            eventPublisher.publishEvent(org.serviceproject.audit.event.AuditEvent.of(
                    principal,
                    org.serviceproject.audit.entity.AuditAction.RESTORE,
                    "Servant",
                    personId,
                    "deleted",
                    "restored: " + person.getFullName()
            ));
        }
    }

    @Transactional
    public ServantResponse createOrEnableAccount(Long personId, String password, UserPrincipal principal) {
        checkCanAddOrEdit(principal);
        AcademicYear currentYear = academicYearService.getCurrentEntity();
        StaffPlacement placement = staffPlacementRepository.findByPersonIdAndAcademicYearId(personId, currentYear.getId())
                .orElseThrow(() -> AppException.notFound("SERVANT_NOT_FOUND", "الخادم غير موجود في العام الدراسي الحالي"));
        checkEditScope(placement, principal);

        Person person = placement.getPerson();
        ensureServantUserAccount(person, password);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(org.serviceproject.audit.event.AuditEvent.of(
                    principal,
                    org.serviceproject.audit.entity.AuditAction.CREATE,
                    "UserAccount",
                    person.getId(),
                    null,
                    "Created/Enabled user account for servant: " + person.getFullName()
            ));
        }

        return toResponse(placement);
    }

    // ── Internal Helpers ─────────────────────────────────────────────

    private void checkCanAddOrEdit(UserPrincipal principal) {
        if (principal.isServant() && !principal.isAdmin() && !principal.isServiceSecretary() && !principal.isClassSecretary()) {
            throw AppException.forbidden("ACCESS_DENIED", "الخادم ليس لديه صلاحية إضافة أو تعديل الخدام");
        }
    }

    private void checkReadScope(StaffPlacement placement, UserPrincipal principal) {
        if (principal.isAdmin()) return;

        if (principal.isServiceSecretary()) {
            if (!placement.getMinistry().getId().equals(principal.getServiceSecretaryMinistryId())) {
                throw AppException.forbidden("ACCESS_DENIED", "الخادم لا يتبع خدمتك المصرح بها");
            }
            return;
        }

        if (principal.isClassSecretary()) {
            if (!placement.getGradeClass().getId().equals(principal.getClassSecretaryClassId())) {
                throw AppException.forbidden("ACCESS_DENIED", "الخادم لا يتبع فصلك المصرح به");
            }
            return;
        }

        if (principal.isServant()) {
            if (!placement.getPerson().getId().equals(principal.getPersonId())) {
                throw AppException.forbidden("ACCESS_DENIED", "يمكنك فقط عرض بياناتك كخادم");
            }
            return;
        }

        throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية لعرض هذا الخادم");
    }

    private void checkEditScope(StaffPlacement placement, UserPrincipal principal) {
        if (principal.isAdmin()) return;

        if (principal.isServiceSecretary()) {
            if (!placement.getMinistry().getId().equals(principal.getServiceSecretaryMinistryId())) {
                throw AppException.forbidden("ACCESS_DENIED", "لا يمكنك تعديل خادم خارج خدمتك المصرح بها");
            }
            return;
        }

        if (principal.isClassSecretary()) {
            if (!placement.getGradeClass().getId().equals(principal.getClassSecretaryClassId())) {
                throw AppException.forbidden("ACCESS_DENIED", "لا يمكنك تعديل خادم خارج فصلك المصرح به");
            }
            return;
        }

        throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية لتعديل هذا الخادم");
    }

    private void checkTargetScope(Long targetMinistryId, Long targetClassId, UserPrincipal principal) {
        if (principal.isAdmin()) return;

        if (principal.isServiceSecretary()) {
            if (!targetMinistryId.equals(principal.getServiceSecretaryMinistryId())) {
                throw AppException.forbidden("ACCESS_DENIED", "لا يمكنك تعيين خادم في خدمة غير خدمتك المصرح بها");
            }
            return;
        }

        if (principal.isClassSecretary()) {
            if (!targetClassId.equals(principal.getClassSecretaryClassId())) {
                throw AppException.forbidden("ACCESS_DENIED", "لا يمكنك تعيين خادم في فصل غير فصلك المصرح به");
            }
            return;
        }

        throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية لتعيين الخادم");
    }

    private GradeClass getGradeClassOrThrow(Long classId) {
        return gradeClassRepository.findByIdWithMinistry(classId)
                .orElseThrow(() -> AppException.notFound("CLASS_NOT_FOUND", "الفصل غير موجود"));
    }

    private void ensureServantUserAccount(Person person, String password) {
        Optional<UserAccount> accountOpt = userAccountRepository.findByPersonId(person.getId());
        UserAccount account;
        if (accountOpt.isPresent()) {
            account = accountOpt.get();
            account.setEnabled(true);
            if (password != null && !password.isBlank()) {
                if (password.trim().equals(person.getPhone().trim())) {
                    throw AppException.badRequest("PASSWORD_CANNOT_BE_PHONE", "لا يمكن استخدام رقم الهاتف ككلمة مرور");
                }
                if (password.trim().length() < 6) {
                    throw AppException.badRequest("PASSWORD_TOO_SHORT", "كلمة المرور يجب ألا تقل عن 6 أحرف");
                }
                account.setPassword(passwordEncoder.encode(password.trim()));
                account.incrementTokenVersion();
            }
        } else {
            if (password == null || password.isBlank()) {
                password = "Pass@" + (100000 + new java.security.SecureRandom().nextInt(900000));
            }
            if (password.trim().equals(person.getPhone().trim())) {
                throw AppException.badRequest("PASSWORD_CANNOT_BE_PHONE", "لا يمكن استخدام رقم الهاتف ككلمة مرور");
            }
            if (password.trim().length() < 6) {
                throw AppException.badRequest("PASSWORD_TOO_SHORT", "كلمة المرور يجب ألا تقل عن 6 أحرف");
            }
            account = new UserAccount();
            account.setPerson(person);
            account.setPassword(passwordEncoder.encode(password.trim()));
            account.setEnabled(true);
        }

        if (!account.hasRole(Role.SERVANT)) {
            account.addRole(new UserRole(Role.SERVANT));
        }

        userAccountRepository.save(account);
    }

    private List<StaffPlacement> getServantsWithSecretaryForClass(Long yearId, Long classId, AcademicYear currentYear) {
        List<StaffPlacement> placements = new java.util.ArrayList<>(
                staffPlacementRepository.findAllActiveByAcademicYearIdAndClassId(yearId, classId)
        );

        if (userAccountRepository != null) {
            List<UserAccount> secretaries = userAccountRepository.findClassSecretariesByClassId(classId);
            for (UserAccount secAcc : secretaries) {
                Person secPerson = secAcc.getPerson();
                if (secPerson == null || secPerson.isDeleted()) continue;

                boolean alreadyInList = placements.stream()
                        .anyMatch(sp -> sp.getPerson() != null && sp.getPerson().getId().equals(secPerson.getId()));

                if (!alreadyInList) {
                    GradeClass gc = getGradeClassOrThrow(classId);
                    StaffPlacement sp = staffPlacementRepository
                            .findByPersonIdAndAcademicYearId(secPerson.getId(), yearId)
                            .orElseGet(() -> new StaffPlacement(secPerson, currentYear, gc.getMinistry(), gc));

                    if (sp.getGradeClass() == null || !sp.getGradeClass().getId().equals(classId)) {
                        sp.setGradeClass(gc);
                        sp.setMinistry(gc.getMinistry());
                    }
                    placements.add(0, sp);
                }
            }
        }

        return placements;
    }

    private ServantResponse toResponse(StaffPlacement sp) {
        Person p = sp.getPerson();
        Optional<UserAccount> acc = userAccountRepository != null ? userAccountRepository.findByPersonId(p.getId()) : Optional.empty();
        List<String> roles = acc.map(u -> u.getRoles().stream().map(r -> r.getRole().name()).toList())
                .orElse(List.of());
        boolean isClassSec = acc.map(u -> u.getRoles().stream()
                .anyMatch(r -> r.getRole() == Role.CLASS_SECRETARY &&
                        (sp.getGradeClass() == null || sp.getGradeClass().getId().equals(r.getClassId()))))
                .orElse(false);

        return new ServantResponse(
                p.getId(),
                acc.map(UserAccount::getId).orElse(null),
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
                sp.getAcademicYear() != null ? sp.getAcademicYear().getId() : null,
                sp.getAcademicYear() != null ? sp.getAcademicYear().getName() : null,
                roles,
                isClassSec,
                !p.isDeleted() && acc.map(UserAccount::isEnabled).orElse(true)
        );
    }
}
