package org.serviceproject.auth.service;

import lombok.RequiredArgsConstructor;
import org.serviceproject.auth.dto.ChangePasswordRequest;
import org.serviceproject.auth.dto.LoginRequest;
import org.serviceproject.auth.dto.LoginResponse;
import org.serviceproject.auth.dto.RoleInfo;
import org.serviceproject.auth.dto.UserInfoResponse;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.JwtProvider;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handles authentication operations: login, password change, and
 * current-user info retrieval.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final org.serviceproject.classes.repository.GradeClassRepository gradeClassRepository;
    private final org.serviceproject.staff.repository.StaffPlacementRepository staffPlacementRepository;
    private final org.serviceproject.academic.service.AcademicYearService academicYearService;

    /**
     * Authenticate by phone + password and return a JWT.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String phone = normalizePhone(request.phone());

        UserAccount account = userAccountRepository.findActiveByPhoneWithRoles(phone)
                .orElseThrow(() -> AppException.unauthorized(
                        "AUTH_FAILED", "رقم الهاتف أو كلمة المرور غير صحيحة"));

        if (!passwordEncoder.matches(request.password(), account.getPassword())) {
            throw AppException.unauthorized(
                    "AUTH_FAILED", "رقم الهاتف أو كلمة المرور غير صحيحة");
        }

        String token = jwtProvider.generateToken(account.getId(), account.getTokenVersion());
        UserInfoResponse userInfo = buildUserInfo(account);

        return new LoginResponse(token, userInfo);
    }

    /**
     * Change the current user's password after verifying the old one.
     * Increments token version to invalidate all existing tokens.
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound(
                        "USER_NOT_FOUND", "المستخدم غير موجود"));

        if (!passwordEncoder.matches(request.oldPassword(), account.getPassword())) {
            throw AppException.badRequest(
                    "WRONG_PASSWORD", "كلمة المرور الحالية غير صحيحة");
        }

        account.setPassword(passwordEncoder.encode(request.newPassword()));
        account.incrementTokenVersion();
        userAccountRepository.save(account);
    }

    /**
     * Get the current authenticated user's info.
     */
    @Transactional(readOnly = true)
    public UserInfoResponse getCurrentUser(Long userId) {
        UserAccount account = userAccountRepository.findActiveByIdWithRoles(userId)
                .orElseThrow(() -> AppException.notFound(
                        "USER_NOT_FOUND", "المستخدم غير موجود"));

        return buildUserInfo(account);
    }

    // ── Internal ─────────────────────────────────────────────────────

    private UserInfoResponse buildUserInfo(UserAccount account) {
        org.serviceproject.academic.entity.AcademicYear currentYear = null;
        try {
            if (academicYearService != null) {
                currentYear = academicYearService.getCurrentEntity();
            }
        } catch (Exception ignored) {}

        org.serviceproject.staff.entity.StaffPlacement servantPlacement = null;
        if (currentYear != null && staffPlacementRepository != null && account.getPerson() != null) {
            servantPlacement = staffPlacementRepository
                    .findAnyByPersonIdAndAcademicYearId(account.getPerson().getId(), currentYear.getId())
                    .orElse(null);
        }

        final org.serviceproject.staff.entity.StaffPlacement placement = servantPlacement;
        List<RoleInfo> roles = account.getRoles().stream()
                .map(ur -> {
                    Long minId = ur.getMinistryId();
                    Long clsId = ur.getClassId();
                    if (ur.getRole() == org.serviceproject.users.entity.Role.CLASS_SECRETARY && clsId != null && minId == null && gradeClassRepository != null) {
                        minId = gradeClassRepository.findById(clsId)
                                .map(c -> c.getMinistry().getId())
                                .orElse(null);
                    }
                    if (ur.getRole() == org.serviceproject.users.entity.Role.SERVANT && placement != null) {
                        if (minId == null) minId = placement.getMinistry().getId();
                        if (clsId == null) clsId = placement.getGradeClass().getId();
                    }
                    return new RoleInfo(ur.getRole().name(), minId, clsId);
                })
                .toList();

        return new UserInfoResponse(
                account.getId(),
                account.getPerson().getId(),
                account.getPerson().getFullName(),
                account.getPerson().getPhone(),
                roles
        );
    }

    /**
     * Basic phone normalization: trim whitespace.
     * No country-specific formatting rules per requirement #5.
     */
    private String normalizePhone(String phone) {
        return phone != null ? phone.trim() : null;
    }
}
