package org.serviceproject.classes.service;

import lombok.RequiredArgsConstructor;
import org.serviceproject.academic.repository.AcademicYearRepository;
import org.serviceproject.audit.entity.AuditAction;
import org.serviceproject.audit.event.AuditEvent;
import org.serviceproject.classes.dto.GradeClassRequest;
import org.serviceproject.classes.dto.GradeClassResponse;
import org.serviceproject.classes.entity.GradeClass;
import org.serviceproject.classes.repository.GradeClassRepository;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.ministries.dto.AssignSecretaryRequest;
import org.serviceproject.ministries.entity.Ministry;
import org.serviceproject.ministries.repository.MinistryRepository;
import org.serviceproject.staff.entity.StaffPlacement;
import org.serviceproject.staff.repository.StaffPlacementRepository;
import org.serviceproject.students.repository.StudentPlacementRepository;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.Role;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.entity.UserRole;
import org.serviceproject.users.repository.PersonRepository;
import org.serviceproject.users.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GradeClass CRUD operations and secretary management.
 */
@Service
@RequiredArgsConstructor
public class GradeClassService {

    private final GradeClassRepository gradeClassRepository;
    private final MinistryRepository ministryRepository;

    @Autowired(required = false)
    private UserAccountRepository userAccountRepository;

    @Autowired(required = false)
    private PersonRepository personRepository;

    @Autowired(required = false)
    private StaffPlacementRepository staffPlacementRepository;

    @Autowired(required = false)
    private StudentPlacementRepository studentPlacementRepository;

    @Autowired(required = false)
    private AcademicYearRepository academicYearRepository;

    @Autowired(required = false)
    private PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<GradeClassResponse> findAll() {
        List<GradeClass> classes = gradeClassRepository.findAllByActiveTrueOrderBySortOrderAsc();
        if (userAccountRepository == null) {
            return classes.stream().map(this::toResponse).toList();
        }
        return enrichClasses(classes);
    }

    @Transactional(readOnly = true)
    public List<GradeClassResponse> findByMinistry(Long ministryId) {
        List<GradeClass> classes = gradeClassRepository.findAllByMinistryIdAndActiveTrueOrderBySortOrderAsc(ministryId);
        if (userAccountRepository == null) {
            return classes.stream().map(this::toResponse).toList();
        }
        return enrichClasses(classes);
    }

    @Transactional(readOnly = true)
    public GradeClassResponse findById(Long id) {
        GradeClass gc = gradeClassRepository.findByIdWithMinistry(id)
                .orElseThrow(() -> AppException.notFound(
                        "CLASS_NOT_FOUND", "الفصل غير موجود"));
        if (userAccountRepository == null) {
            return toResponse(gc);
        }
        return enrichClasses(List.of(gc)).get(0);
    }

    @Transactional
    public GradeClassResponse create(GradeClassRequest request) {
        return create(request, null);
    }

    @Transactional
    public GradeClassResponse create(GradeClassRequest request, UserPrincipal principal) {
        validateSecretaryMinistryScope(request.ministryId(), principal);

        Ministry ministry = ministryRepository.findById(request.ministryId())
                .orElseThrow(() -> AppException.notFound(
                        "MINISTRY_NOT_FOUND", "الخدمة غير موجودة"));

        String name = request.name().trim();
        if (gradeClassRepository.existsByMinistryIdAndName(ministry.getId(), name)) {
            throw AppException.conflict("CLASS_EXISTS",
                    "فصل بهذا الاسم موجود بالفعل في هذه الخدمة");
        }

        GradeClass gc = new GradeClass(name, ministry);
        gc.setSortOrder(request.sortOrder());
        gc = gradeClassRepository.save(gc);
        if (userAccountRepository == null) {
            return toResponse(gc);
        }
        return enrichClasses(List.of(gc)).get(0);
    }

    @Transactional
    public GradeClassResponse update(Long id, GradeClassRequest request) {
        return update(id, request, null);
    }

    @Transactional
    public GradeClassResponse update(Long id, GradeClassRequest request, UserPrincipal principal) {
        GradeClass gc = gradeClassRepository.findByIdWithMinistry(id)
                .orElseThrow(() -> AppException.notFound(
                        "CLASS_NOT_FOUND", "الفصل غير موجود"));

        validateSecretaryMinistryScope(gc.getMinistry().getId(), principal);
        validateSecretaryMinistryScope(request.ministryId(), principal);

        Ministry ministry = ministryRepository.findById(request.ministryId())
                .orElseThrow(() -> AppException.notFound(
                        "MINISTRY_NOT_FOUND", "الخدمة غير موجودة"));

        String newName = request.name().trim();
        boolean nameChanged = !gc.getName().equals(newName);
        boolean ministryChanged = !gc.getMinistry().getId().equals(ministry.getId());

        if ((nameChanged || ministryChanged)
                && gradeClassRepository.existsByMinistryIdAndName(ministry.getId(), newName)) {
            throw AppException.conflict("CLASS_EXISTS",
                    "فصل بهذا الاسم موجود بالفعل في هذه الخدمة");
        }

        gc.setName(newName);
        gc.setMinistry(ministry);
        gc.setSortOrder(request.sortOrder());
        gc = gradeClassRepository.save(gc);
        if (userAccountRepository == null) {
            return toResponse(gc);
        }
        return enrichClasses(List.of(gc)).get(0);
    }

    @Transactional
    public void deactivate(Long id) {
        deactivate(id, null);
    }

    @Transactional
    public void deactivate(Long id, UserPrincipal principal) {
        GradeClass gc = gradeClassRepository.findById(id)
                .orElseThrow(() -> AppException.notFound(
                        "CLASS_NOT_FOUND", "الفصل غير موجود"));

        validateSecretaryMinistryScope(gc.getMinistry().getId(), principal);

        gc.setActive(false);
        gradeClassRepository.save(gc);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(AuditEvent.of(
                    principal,
                    AuditAction.DELETE,
                    "GradeClass",
                    id,
                    "Deactivated class: " + gc.getName(),
                    null
            ));
        }
    }

    @Transactional
    public void assignSecretary(Long classId, AssignSecretaryRequest request, UserPrincipal principal) {
        GradeClass gc = gradeClassRepository.findByIdWithMinistry(classId)
                .orElseThrow(() -> AppException.notFound("CLASS_NOT_FOUND", "الفصل غير موجود"));

        validateSecretaryMinistryScope(gc.getMinistry().getId(), principal);

        if (userAccountRepository == null || personRepository == null) {
            return;
        }

        // 1. Clear existing class secretary
        List<UserAccount> existing = userAccountRepository.findClassSecretariesByClassId(classId);
        for (UserAccount ua : existing) {
            ua.getRoles().removeIf(r -> r.getRole() == Role.CLASS_SECRETARY && classId.equals(r.getClassId()));
            userAccountRepository.save(ua);
        }

        // 2. Assign new secretary if requested
        if (request != null && (request.userId() != null || request.personId() != null)) {
            UserAccount account;
            if (request.userId() != null) {
                account = userAccountRepository.findById(request.userId())
                        .orElseThrow(() -> AppException.notFound("USER_NOT_FOUND", "المستخدم غير موجود"));
            } else {
                Person person = personRepository.findById(request.personId())
                        .orElseThrow(() -> AppException.notFound("PERSON_NOT_FOUND", "الخادم غير موجود"));
                account = userAccountRepository.findByPersonId(person.getId())
                        .orElseGet(() -> {
                            UserAccount newAcc = new UserAccount();
                            newAcc.setPerson(person);
                            if (passwordEncoder != null) {
                                String randomPass = "Sec@" + java.util.UUID.randomUUID().toString().substring(0, 8);
                                newAcc.setPassword(passwordEncoder.encode(randomPass));
                            }
                            newAcc.setEnabled(true);
                            return userAccountRepository.save(newAcc);
                        });
            }

            boolean hasRole = account.getRoles().stream()
                    .anyMatch(r -> r.getRole() == Role.CLASS_SECRETARY && classId.equals(r.getClassId()));
            if (!hasRole) {
                UserRole newRole = new UserRole(Role.CLASS_SECRETARY);
                newRole.setClassId(classId);
                newRole.setMinistryId(gc.getMinistry().getId());
                account.addRole(newRole);
            }
            if (!account.hasRole(Role.SERVANT)) {
                account.addRole(new UserRole(Role.SERVANT));
            }
            userAccountRepository.save(account);

            if (academicYearRepository != null && staffPlacementRepository != null) {
                academicYearRepository.findByCurrentTrue().ifPresent(currYear -> {
                    StaffPlacement staffPlacement = staffPlacementRepository
                            .findByPersonIdAndAcademicYearId(account.getPerson().getId(), currYear.getId())
                            .orElseGet(() -> new StaffPlacement(account.getPerson(), currYear, gc.getMinistry(), gc));
                    staffPlacement.setMinistry(gc.getMinistry());
                    staffPlacement.setGradeClass(gc);
                    staffPlacementRepository.save(staffPlacement);
                });
            }

            if (eventPublisher != null) {
                eventPublisher.publishEvent(AuditEvent.of(
                        principal,
                        AuditAction.ROLE_CHANGE,
                        "GradeClass",
                        classId,
                        "Assigned class secretary: " + account.getPerson().getFullName(),
                        "CLASS_SECRETARY"
                ));
            }
        }
    }

    @Transactional
    public void removeSecretary(Long classId, UserPrincipal principal) {
        assignSecretary(classId, null, principal);
    }

    // ── Internal Helpers ─────────────────────────────────────────────

    private void validateSecretaryMinistryScope(Long ministryId, UserPrincipal principal) {
        if (principal == null || principal.isAdmin()) {
            return;
        }
        if (principal.isServiceSecretary()) {
            Long managed = principal.getServiceSecretaryMinistryId();
            if (managed != null && managed.equals(ministryId)) {
                return;
            }
        }
        throw AppException.forbidden("ACCESS_DENIED", "لا تملك صلاحية لإدارة فصول هذه الخدمة");
    }

    private GradeClassResponse toResponse(GradeClass gc) {
        return new GradeClassResponse(
                gc.getId(),
                gc.getName(),
                gc.getMinistry().getId(),
                gc.getMinistry().getName(),
                gc.isActive(),
                gc.getSortOrder()
        );
    }

    private List<GradeClassResponse> enrichClasses(List<GradeClass> classes) {
        if (classes.isEmpty()) {
            return List.of();
        }

        Map<Long, UserAccount> secretaryMap = new HashMap<>();
        if (userAccountRepository != null) {
            for (UserAccount ua : userAccountRepository.findAllClassSecretaries()) {
                for (UserRole r : ua.getRoles()) {
                    if (r.getRole() == Role.CLASS_SECRETARY && r.getClassId() != null) {
                        secretaryMap.put(r.getClassId(), ua);
                    }
                }
            }
        }

        Map<Long, Long> servantCountMap = new HashMap<>();
        Map<Long, Long> studentCountMap = new HashMap<>();
        if (academicYearRepository != null && staffPlacementRepository != null && studentPlacementRepository != null) {
            academicYearRepository.findByCurrentTrue().ifPresent(year -> {
                Long yId = year.getId();
                for (Object[] row : staffPlacementRepository.countActiveServantsPerClass(yId)) {
                    Long cId = (Long) row[0];
                    Long count = (Long) row[1];
                    if (cId != null) servantCountMap.put(cId, count);
                }
                for (Object[] row : studentPlacementRepository.countActiveStudentsPerClass(yId)) {
                    Long cId = (Long) row[0];
                    Long count = (Long) row[1];
                    if (cId != null) studentCountMap.put(cId, count);
                }
            });
        }

        return classes.stream().map(gc -> {
            UserAccount sec = secretaryMap.get(gc.getId());
            return new GradeClassResponse(
                    gc.getId(),
                    gc.getName(),
                    gc.getMinistry().getId(),
                    gc.getMinistry().getName(),
                    gc.isActive(),
                    gc.getSortOrder(),
                    sec != null ? sec.getId() : null,
                    sec != null ? sec.getPerson().getId() : null,
                    sec != null ? sec.getPerson().getFullName() : null,
                    sec != null ? sec.getPerson().getPhone() : null,
                    servantCountMap.getOrDefault(gc.getId(), 0L),
                    studentCountMap.getOrDefault(gc.getId(), 0L)
            );
        }).toList();
    }
}
