package org.serviceproject.ministries.service;

import lombok.RequiredArgsConstructor;
import org.serviceproject.academic.repository.AcademicYearRepository;
import org.serviceproject.audit.entity.AuditAction;
import org.serviceproject.audit.event.AuditEvent;
import org.serviceproject.classes.repository.GradeClassRepository;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.ministries.dto.AssignSecretaryRequest;
import org.serviceproject.ministries.dto.MinistryRequest;
import org.serviceproject.ministries.dto.MinistryResponse;
import org.serviceproject.ministries.entity.Ministry;
import org.serviceproject.ministries.mapper.MinistryMapper;
import org.serviceproject.ministries.repository.MinistryRepository;
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
 * Ministry CRUD operations and secretary assignments.
 */
@Service
@RequiredArgsConstructor
public class MinistryService {

    private final MinistryRepository ministryRepository;
    private final MinistryMapper ministryMapper;

    @Autowired(required = false)
    private UserAccountRepository userAccountRepository;

    @Autowired(required = false)
    private PersonRepository personRepository;

    @Autowired(required = false)
    private GradeClassRepository gradeClassRepository;

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
    public List<MinistryResponse> findAll() {
        List<Ministry> ministries = ministryRepository.findAllByOrderByNameAsc();
        if (userAccountRepository == null) {
            return ministryMapper.toResponseList(ministries);
        }
        return enrichMinistries(ministries);
    }

    @Transactional(readOnly = true)
    public List<MinistryResponse> findActive() {
        List<Ministry> ministries = ministryRepository.findAllByActiveTrue();
        if (userAccountRepository == null) {
            return ministryMapper.toResponseList(ministries);
        }
        return enrichMinistries(ministries);
    }

    @Transactional(readOnly = true)
    public MinistryResponse findById(Long id) {
        Ministry ministry = getOrThrow(id);
        if (userAccountRepository == null) {
            return ministryMapper.toResponse(ministry);
        }
        List<MinistryResponse> enriched = enrichMinistries(List.of(ministry));
        return enriched.get(0);
    }

    @Transactional
    public MinistryResponse create(MinistryRequest request) {
        if (ministryRepository.existsByName(request.name().trim())) {
            throw AppException.conflict("MINISTRY_EXISTS",
                    "خدمة بهذا الاسم موجودة بالفعل");
        }

        Ministry ministry = ministryMapper.toEntity(request);
        ministry.setName(request.name().trim());
        ministry = ministryRepository.save(ministry);
        if (userAccountRepository == null) {
            return ministryMapper.toResponse(ministry);
        }
        return enrichMinistries(List.of(ministry)).get(0);
    }

    @Transactional
    public MinistryResponse update(Long id, MinistryRequest request) {
        Ministry ministry = getOrThrow(id);

        String newName = request.name().trim();
        if (!ministry.getName().equals(newName)
                && ministryRepository.existsByName(newName)) {
            throw AppException.conflict("MINISTRY_EXISTS",
                    "خدمة بهذا الاسم موجودة بالفعل");
        }

        ministry.setName(newName);
        ministry = ministryRepository.save(ministry);
        if (userAccountRepository == null) {
            return ministryMapper.toResponse(ministry);
        }
        return enrichMinistries(List.of(ministry)).get(0);
    }

    @Transactional
    public void deactivate(Long id) {
        Ministry ministry = getOrThrow(id);
        ministry.setActive(false);
        ministryRepository.save(ministry);
    }

    @Transactional
    public void assignSecretary(Long ministryId, AssignSecretaryRequest request, UserPrincipal principal) {
        getOrThrow(ministryId);

        if (userAccountRepository == null || personRepository == null) {
            return;
        }

        // 1. Clear existing secretary for this ministry
        List<UserAccount> existing = userAccountRepository.findServiceSecretariesByMinistryId(ministryId);
        for (UserAccount ua : existing) {
            ua.getRoles().removeIf(r -> r.getRole() == Role.SERVICE_SECRETARY && ministryId.equals(r.getMinistryId()));
            userAccountRepository.save(ua);
        }

        // 2. If new secretary specified, assign role
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
                    .anyMatch(r -> r.getRole() == Role.SERVICE_SECRETARY && ministryId.equals(r.getMinistryId()));
            if (!hasRole) {
                UserRole newRole = new UserRole(Role.SERVICE_SECRETARY);
                newRole.setMinistryId(ministryId);
                account.addRole(newRole);
            }
            if (!account.hasRole(Role.SERVANT)) {
                account.addRole(new UserRole(Role.SERVANT));
            }
            userAccountRepository.save(account);

            if (eventPublisher != null) {
                eventPublisher.publishEvent(AuditEvent.of(
                        principal,
                        AuditAction.ROLE_CHANGE,
                        "Ministry",
                        ministryId,
                        "Assigned secretary: " + account.getPerson().getFullName(),
                        "SERVICE_SECRETARY"
                ));
            }
        }
    }

    @Transactional
    public void removeSecretary(Long ministryId, UserPrincipal principal) {
        assignSecretary(ministryId, null, principal);
    }

    // ── Internal Helpers ─────────────────────────────────────────────

    private List<MinistryResponse> enrichMinistries(List<Ministry> ministries) {
        if (ministries.isEmpty()) {
            return List.of();
        }

        Map<Long, UserAccount> secretaryMap = new HashMap<>();
        if (userAccountRepository != null) {
            for (UserAccount ua : userAccountRepository.findAllServiceSecretaries()) {
                for (UserRole r : ua.getRoles()) {
                    if (r.getRole() == Role.SERVICE_SECRETARY && r.getMinistryId() != null) {
                        secretaryMap.put(r.getMinistryId(), ua);
                    }
                }
            }
        }

        Map<Long, Long> classCountMap = new HashMap<>();
        if (gradeClassRepository != null) {
            for (Object[] row : gradeClassRepository.countActiveClassesPerMinistry()) {
                Long ministryId = (Long) row[0];
                Long count = (Long) row[1];
                if (ministryId != null) classCountMap.put(ministryId, count);
            }
        }

        Map<Long, Long> servantCountMap = new HashMap<>();
        Map<Long, Long> studentCountMap = new HashMap<>();
        if (academicYearRepository != null && staffPlacementRepository != null && studentPlacementRepository != null) {
            academicYearRepository.findByCurrentTrue().ifPresent(year -> {
                Long yId = year.getId();
                for (Object[] row : staffPlacementRepository.countActiveServantsPerMinistry(yId)) {
                    Long mId = (Long) row[0];
                    Long count = (Long) row[1];
                    if (mId != null) servantCountMap.put(mId, count);
                }
                for (Object[] row : studentPlacementRepository.countActiveStudentsPerMinistry(yId)) {
                    Long mId = (Long) row[0];
                    Long count = (Long) row[1];
                    if (mId != null) studentCountMap.put(mId, count);
                }
            });
        }

        return ministries.stream().map(m -> {
            UserAccount sec = secretaryMap.get(m.getId());
            return new MinistryResponse(
                    m.getId(),
                    m.getName(),
                    m.isActive(),
                    sec != null ? sec.getId() : null,
                    sec != null ? sec.getPerson().getId() : null,
                    sec != null ? sec.getPerson().getFullName() : null,
                    sec != null ? sec.getPerson().getPhone() : null,
                    classCountMap.getOrDefault(m.getId(), 0L),
                    servantCountMap.getOrDefault(m.getId(), 0L),
                    studentCountMap.getOrDefault(m.getId(), 0L)
            );
        }).toList();
    }

    private Ministry getOrThrow(Long id) {
        return ministryRepository.findById(id)
                .orElseThrow(() -> AppException.notFound(
                        "MINISTRY_NOT_FOUND", "الخدمة غير موجودة"));
    }
}
