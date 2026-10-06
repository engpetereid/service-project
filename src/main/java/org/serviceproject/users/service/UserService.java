package org.serviceproject.users.service;

import lombok.RequiredArgsConstructor;
import org.serviceproject.auth.dto.RoleInfo;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.users.dto.AssignRolesRequest;
import org.serviceproject.users.dto.CreateUserRequest;
import org.serviceproject.users.dto.UpdateUserRequest;
import org.serviceproject.users.dto.UserResponse;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.Role;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.entity.UserRole;
import org.serviceproject.users.repository.PersonRepository;
import org.serviceproject.users.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * User management operations (admin only).
 * <p>
 * Manages person data, user accounts, and role assignments.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final PersonRepository personRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userAccountRepository.findAllActiveUsers().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String phone = request.phone().trim();

        Person person;
        if (personRepository.existsByPhone(phone)) {
            java.util.Optional<Person> existingOpt = personRepository.findByPhone(phone);
            if (existingOpt != null && existingOpt.isPresent() && existingOpt.get().isDeleted()) {
                Person existing = existingOpt.get();
                person = existing;
                person.restore();
                person.setFullName(request.fullName().trim());
                person.setGender(request.gender());
                person.setDateOfBirth(request.dateOfBirth());
                person.setAddress(request.address());
                person.setConfessionFather(request.confessionFather());
                person = personRepository.save(person);
            } else {
                throw AppException.conflict("PHONE_EXISTS", "رقم الهاتف مستخدم بالفعل");
            }
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

        java.util.Optional<UserAccount> existingAcc = userAccountRepository.findByPersonId(person.getId());
        UserAccount account;
        if (existingAcc.isPresent()) {
            account = existingAcc.get();
            account.setPassword(passwordEncoder.encode(request.password()));
            account.setEnabled(true);
            account.incrementTokenVersion();
        } else {
            account = new UserAccount();
            account.setPerson(person);
            account.setPassword(passwordEncoder.encode(request.password()));
            account.setEnabled(true);
        }
        account = userAccountRepository.save(account);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(org.serviceproject.audit.event.AuditEvent.system(
                    org.serviceproject.audit.entity.AuditAction.CREATE,
                    "UserAccount",
                    account.getId(),
                    null,
                    "Created user: " + person.getFullName() + " (" + phone + ")"
            ));
        }

        return toResponse(account);
    }

    @Transactional
    public UserResponse update(Long userId, UpdateUserRequest request) {
        UserAccount account = getAccountOrThrow(userId);
        Person person = account.getPerson();
        String newPhone = request.phone().trim();

        // Phone uniqueness check (if changed)
        if (!person.getPhone().equals(newPhone) && personRepository.existsByPhone(newPhone)) {
            throw AppException.conflict("PHONE_EXISTS",
                    "رقم الهاتف مستخدم بالفعل");
        }

        boolean phoneChanged = !person.getPhone().equals(newPhone);

        person.setFullName(request.fullName().trim());
        person.setPhone(newPhone);
        person.setGender(request.gender());
        person.setDateOfBirth(request.dateOfBirth());
        person.setAddress(request.address());
        person.setConfessionFather(request.confessionFather());
        personRepository.save(person);

        // Invalidate tokens if phone changed (requirement #5.1)
        if (phoneChanged) {
            account.incrementTokenVersion();
            userAccountRepository.save(account);
        }

        return toResponse(account);
    }

    @Transactional
    public UserResponse assignRoles(Long userId, AssignRolesRequest request) {
        UserAccount account = userAccountRepository.findActiveByIdWithRoles(userId)
                .orElseThrow(() -> AppException.notFound(
                        "USER_NOT_FOUND", "المستخدم غير موجود"));

        // Validate scope requirements
        for (AssignRolesRequest.RoleAssignment ra : request.roles()) {
            validateRoleScope(ra);
        }

        List<String> oldRoles = account.getRoles().stream()
                .map(r -> r.getRole().name() + (r.getMinistryId() != null ? ":" + r.getMinistryId() : ""))
                .toList();

        // Clear existing roles and set new ones
        account.getRoles().clear();
        for (AssignRolesRequest.RoleAssignment ra : request.roles()) {
            UserRole userRole = new UserRole(ra.role(), ra.ministryId(), ra.classId());
            account.addRole(userRole);
        }

        account = userAccountRepository.save(account);

        List<String> newRoles = account.getRoles().stream()
                .map(r -> r.getRole().name() + (r.getMinistryId() != null ? ":" + r.getMinistryId() : ""))
                .toList();

        if (eventPublisher != null) {
            eventPublisher.publishEvent(org.serviceproject.audit.event.AuditEvent.system(
                    org.serviceproject.audit.entity.AuditAction.ROLE_CHANGE,
                    "UserAccount",
                    userId,
                    oldRoles,
                    newRoles
            ));
        }

        return toResponse(account);
    }

    @Transactional
    public void toggleActive(Long userId) {
        UserAccount account = getAccountOrThrow(userId);
        boolean oldStatus = account.isEnabled();
        account.setEnabled(!oldStatus);

        // If disabling, invalidate tokens
        if (!account.isEnabled()) {
            account.incrementTokenVersion();
        }

        userAccountRepository.save(account);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(org.serviceproject.audit.event.AuditEvent.system(
                    org.serviceproject.audit.entity.AuditAction.STATUS_CHANGE,
                    "UserAccount",
                    userId,
                    "enabled: " + oldStatus,
                    "enabled: " + account.isEnabled()
            ));
        }
    }

    @Transactional
    public void restoreDeleted(Long userId) {
        UserAccount account = getAccountOrThrow(userId);
        Person person = account.getPerson();

        if (!person.isDeleted()) {
            throw AppException.badRequest("NOT_DELETED",
                    "هذا المستخدم غير محذوف");
        }

        person.restore();
        personRepository.save(person);

        account.setEnabled(true);
        userAccountRepository.save(account);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(org.serviceproject.audit.event.AuditEvent.system(
                    org.serviceproject.audit.entity.AuditAction.RESTORE,
                    "UserAccount",
                    userId,
                    "deleted",
                    "restored"
            ));
        }
    }

    @Transactional
    public void delete(Long userId, UserPrincipal principal) {
        UserAccount account = getAccountOrThrow(userId);
        Person person = account.getPerson();

        if (principal != null && principal.getUserId().equals(userId)) {
            throw AppException.badRequest("CANNOT_DELETE_SELF", "لا يمكنك حذف حسابك الشخصي الحالي");
        }

        if (account.hasRole(Role.GENERAL_ADMIN)) {
            long adminCount = userAccountRepository.countActiveGeneralAdmins();
            if (adminCount <= 1) {
                throw AppException.badRequest("CANNOT_DELETE_LAST_ADMIN", "لا يمكن حذف آخر مسؤول عام (أدمن) في النظام");
            }
        }

        // Soft delete person
        person.softDelete();
        personRepository.save(person);

        // Deactivate account and invalidate tokens
        account.setEnabled(false);
        account.incrementTokenVersion();
        account.getRoles().clear();
        userAccountRepository.save(account);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(org.serviceproject.audit.event.AuditEvent.system(
                    org.serviceproject.audit.entity.AuditAction.DELETE,
                    "UserAccount",
                    userId,
                    person.getFullName() + " (" + person.getPhone() + ")",
                    "Deleted user account and soft-deleted person"
            ));
        }
    }

    @Transactional
    public UserResponse createForPerson(Long personId, String rawPassword, List<AssignRolesRequest.RoleAssignment> roles) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> AppException.notFound("PERSON_NOT_FOUND", "الشخص غير موجود"));

        if (userAccountRepository.findByPersonId(personId).isPresent()) {
            throw AppException.conflict("ACCOUNT_EXISTS", "يوجد حساب مستخدم بالفعل لهذا الشخص");
        }

        if (rawPassword == null || rawPassword.isBlank()) {
            throw AppException.badRequest("PASSWORD_REQUIRED", "كلمة المرور مطلوبة لإنشاء حساب المستخدم");
        }
        if (rawPassword.trim().equals(person.getPhone().trim())) {
            throw AppException.badRequest("PASSWORD_CANNOT_BE_PHONE", "لا يمكن استخدام رقم الهاتف ككلمة مرور");
        }
        if (rawPassword.trim().length() < 6) {
            throw AppException.badRequest("PASSWORD_TOO_SHORT", "كلمة المرور يجب ألا تقل عن 6 أحرف");
        }

        UserAccount account = new UserAccount();
        account.setPerson(person);
        account.setPassword(passwordEncoder.encode(rawPassword.trim()));
        account.setEnabled(true);

        if (roles != null && !roles.isEmpty()) {
            for (AssignRolesRequest.RoleAssignment ra : roles) {
                validateRoleScope(ra);
                account.addRole(new UserRole(ra.role(), ra.ministryId(), ra.classId()));
            }
        } else {
            account.addRole(new UserRole(Role.SERVANT));
        }

        account = userAccountRepository.save(account);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(org.serviceproject.audit.event.AuditEvent.system(
                    org.serviceproject.audit.entity.AuditAction.CREATE,
                    "UserAccount",
                    account.getId(),
                    null,
                    "Created user account for existing person: " + person.getFullName() + " (" + person.getPhone() + ")"
            ));
        }

        return toResponse(account);
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        UserAccount account = getAccountOrThrow(userId);
        Person person = account.getPerson();

        if (newPassword == null || newPassword.isBlank()) {
            throw AppException.badRequest("PASSWORD_REQUIRED", "كلمة المرور الجديدة مطلوبة");
        }
        if (newPassword.trim().equals(person.getPhone().trim())) {
            throw AppException.badRequest("PASSWORD_CANNOT_BE_PHONE", "لا يمكن استخدام رقم الهاتف ككلمة مرور");
        }
        if (newPassword.trim().length() < 6) {
            throw AppException.badRequest("PASSWORD_TOO_SHORT", "كلمة المرور يجب ألا تقل عن 6 أحرف");
        }

        account.setPassword(passwordEncoder.encode(newPassword.trim()));
        account.incrementTokenVersion();
        userAccountRepository.save(account);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(org.serviceproject.audit.event.AuditEvent.system(
                    org.serviceproject.audit.entity.AuditAction.STATUS_CHANGE,
                    "UserAccount",
                    userId,
                    "password_changed",
                    "reset by admin"
            ));
        }
    }

    // ── Internal ─────────────────────────────────────────────────────

    private void validateRoleScope(AssignRolesRequest.RoleAssignment ra) {
        if (ra.role() == Role.SERVICE_SECRETARY && ra.ministryId() == null) {
            throw AppException.badRequest("MINISTRY_REQUIRED",
                    "أمين الخدمة يحتاج تحديد الخدمة");
        }
        if (ra.role() == Role.CLASS_SECRETARY && ra.classId() == null) {
            throw AppException.badRequest("CLASS_REQUIRED",
                    "أمين الفصل يحتاج تحديد الفصل");
        }
    }

    private UserAccount getAccountOrThrow(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound(
                        "USER_NOT_FOUND", "المستخدم غير موجود"));
    }

    private UserResponse toResponse(UserAccount account) {
        Person person = account.getPerson();
        List<RoleInfo> roles = account.getRoles().stream()
                .map(ur -> new RoleInfo(
                        ur.getRole().name(),
                        ur.getMinistryId(),
                        ur.getClassId()))
                .toList();

        return new UserResponse(
                account.getId(),
                person.getId(),
                person.getFullName(),
                person.getPhone(),
                person.getGender().name(),
                person.getDateOfBirth(),
                person.getAddress(),
                person.getConfessionFather(),
                account.isEnabled(),
                roles
        );
    }
}
