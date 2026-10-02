package org.serviceproject.search.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.academic.service.AcademicYearService;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.search.dto.SearchResultResponse;
import org.serviceproject.staff.entity.StaffPlacement;
import org.serviceproject.staff.repository.StaffPlacementRepository;
import org.serviceproject.students.entity.StudentPlacement;
import org.serviceproject.students.entity.StudentStatus;
import org.serviceproject.students.repository.StudentPlacementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service providing fast global search across servants and students
 * with role-based scope isolation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final StudentPlacementRepository studentPlacementRepository;
    private final StaffPlacementRepository staffPlacementRepository;
    private final AcademicYearService academicYearService;

    /**
     * Performs a global search across servants and students matching the query,
     * strictly enforcing role-based scoping.
     */
    @Transactional(readOnly = true)
    public List<SearchResultResponse> searchPeople(String query, UserPrincipal principal) {
        if (principal == null) {
            throw AppException.unauthorized("UNAUTHORIZED", "يرجى تسجيل الدخول");
        }

        if (query == null || query.trim().length() < 2) {
            return Collections.emptyList();
        }

        String trimmedQuery = query.trim().toLowerCase();
        AcademicYear currentYear = academicYearService.getCurrentEntity();
        if (currentYear == null) {
            return Collections.emptyList();
        }

        List<SearchResultResponse> results = new ArrayList<>();

        // 1. Search Servants (StaffPlacements)
        List<StaffPlacement> staffPlacements = staffPlacementRepository.findAllActiveByAcademicYearId(currentYear.getId());
        for (StaffPlacement sp : staffPlacements) {
            String name = sp.getPerson().getFullName();
            String phone = sp.getPerson().getPhone();
            boolean matches = (name != null && name.toLowerCase().contains(trimmedQuery)) ||
                              (phone != null && phone.contains(trimmedQuery));

            if (matches && isStaffInScope(sp, principal)) {
                results.add(new SearchResultResponse(
                        sp.getPerson().getId(),
                        sp.getPerson().getFullName(),
                        sp.getPerson().getPhone(),
                        "خادم",
                        sp.getMinistry() != null ? sp.getMinistry().getId() : null,
                        sp.getMinistry() != null ? sp.getMinistry().getName() : "",
                        sp.getGradeClass() != null ? sp.getGradeClass().getId() : null,
                        sp.getGradeClass() != null ? sp.getGradeClass().getName() : "",
                        null,
                        "",
                        "نشط"
                ));
            }
        }

        // 2. Search Students (StudentPlacements)
        List<StudentPlacement> studentPlacements = studentPlacementRepository.searchByAcademicYearIdAndStatus(
                currentYear.getId(), trimmedQuery, StudentStatus.ACTIVE);

        for (StudentPlacement sp : studentPlacements) {
            if (isStudentInScope(sp, principal)) {
                results.add(new SearchResultResponse(
                        sp.getPerson().getId(),
                        sp.getPerson().getFullName(),
                        sp.getPerson().getPhone(),
                        "مخدوم",
                        sp.getMinistry() != null ? sp.getMinistry().getId() : null,
                        sp.getMinistry() != null ? sp.getMinistry().getName() : "",
                        sp.getGradeClass() != null ? sp.getGradeClass().getId() : null,
                        sp.getGradeClass() != null ? sp.getGradeClass().getName() : "",
                        sp.getResponsibleServant() != null ? sp.getResponsibleServant().getId() : null,
                        sp.getResponsibleServant() != null ? sp.getResponsibleServant().getFullName() : "",
                        sp.getStatus() == StudentStatus.ACTIVE ? "نشط" : "خريج"
                ));
            }
        }

        return results;
    }

    private boolean isStaffInScope(StaffPlacement sp, UserPrincipal principal) {
        if (principal.isAdmin()) return true;
        if (principal.isServiceSecretary()) {
            return sp.getMinistry() != null && sp.getMinistry().getId().equals(principal.getServiceSecretaryMinistryId());
        }
        if (principal.isClassSecretary()) {
            return sp.getGradeClass() != null && sp.getGradeClass().getId().equals(principal.getClassSecretaryClassId());
        }
        if (principal.isServant()) {
            return sp.getPerson().getId().equals(principal.getPersonId());
        }
        return false;
    }

    private boolean isStudentInScope(StudentPlacement sp, UserPrincipal principal) {
        if (principal.isAdmin()) return true;
        if (principal.isServiceSecretary()) {
            return sp.getMinistry() != null && sp.getMinistry().getId().equals(principal.getServiceSecretaryMinistryId());
        }
        if (principal.isClassSecretary()) {
            return sp.getGradeClass() != null && sp.getGradeClass().getId().equals(principal.getClassSecretaryClassId());
        }
        if (principal.isServant()) {
            return sp.getResponsibleServant() != null && sp.getResponsibleServant().getId().equals(principal.getPersonId());
        }
        return false;
    }
}
