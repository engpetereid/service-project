package org.serviceproject.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.auth.dto.ChangePasswordRequest;
import org.serviceproject.auth.dto.LoginRequest;
import org.serviceproject.auth.dto.LoginResponse;
import org.serviceproject.auth.dto.UserInfoResponse;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.JwtProvider;
import org.serviceproject.users.entity.Gender;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.Role;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.entity.UserRole;
import org.serviceproject.users.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthService}.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;

    private Person testPerson;
    private UserAccount testAccount;

    @BeforeEach
    void setUp() {
        testPerson = new Person();
        testPerson.setId(10L);
        testPerson.setFullName("Peter");
        testPerson.setPhone("01234567890");
        testPerson.setGender(Gender.MALE);

        testAccount = new UserAccount();
        testAccount.setId(1L);
        testAccount.setPerson(testPerson);
        testAccount.setPassword("hashedPassword");
        testAccount.setEnabled(true);
        testAccount.setTokenVersion(0);

        UserRole adminRole = new UserRole(Role.GENERAL_ADMIN);
        adminRole.setUser(testAccount);
        testAccount.getRoles().add(adminRole);
    }

    // ── Login ────────────────────────────────────────────────────────

    @Test
    void login_success_returnsTokenAndUserInfo() {
        when(userAccountRepository.findActiveByPhoneWithRoles("01234567890"))
                .thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches("password123", "hashedPassword"))
                .thenReturn(true);
        when(jwtProvider.generateToken(1L, 0))
                .thenReturn("jwt-token-here");

        LoginResponse response = authService.login(
                new LoginRequest("01234567890", "password123"));

        assertNotNull(response);
        assertEquals("jwt-token-here", response.token());
        assertNotNull(response.user());
        assertEquals("Peter", response.user().fullName());
        assertEquals("01234567890", response.user().phone());
        assertEquals(1, response.user().roles().size());
        assertEquals("GENERAL_ADMIN", response.user().roles().get(0).role());
    }

    @Test
    void login_wrongPhone_throwsUnauthorized() {
        when(userAccountRepository.findActiveByPhoneWithRoles("09999999999"))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                authService.login(new LoginRequest("09999999999", "password123")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("AUTH_FAILED", ex.getCode());
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        when(userAccountRepository.findActiveByPhoneWithRoles("01234567890"))
                .thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches("wrongpass", "hashedPassword"))
                .thenReturn(false);

        AppException ex = assertThrows(AppException.class, () ->
                authService.login(new LoginRequest("01234567890", "wrongpass")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("AUTH_FAILED", ex.getCode());
    }

    @Test
    void login_sameErrorForWrongPhoneAndPassword() {
        // Should not reveal whether phone or password was wrong
        when(userAccountRepository.findActiveByPhoneWithRoles("01234567890"))
                .thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches("wrongpass", "hashedPassword"))
                .thenReturn(false);

        AppException wrongPass = assertThrows(AppException.class, () ->
                authService.login(new LoginRequest("01234567890", "wrongpass")));

        when(userAccountRepository.findActiveByPhoneWithRoles("09999999999"))
                .thenReturn(Optional.empty());

        AppException wrongPhone = assertThrows(AppException.class, () ->
                authService.login(new LoginRequest("09999999999", "password123")));

        // Both should have the same error code (no info leakage)
        assertEquals(wrongPass.getCode(), wrongPhone.getCode());
    }

    @Test
    void login_trimmedPhone_isUsed() {
        when(userAccountRepository.findActiveByPhoneWithRoles("01234567890"))
                .thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches("password123", "hashedPassword"))
                .thenReturn(true);
        when(jwtProvider.generateToken(1L, 0)).thenReturn("token");

        authService.login(new LoginRequest("  01234567890  ", "password123"));

        verify(userAccountRepository).findActiveByPhoneWithRoles("01234567890");
    }

    // ── Change Password ──────────────────────────────────────────────

    @Test
    void changePassword_success_updatesPasswordAndIncrVersions() {
        when(userAccountRepository.findById(1L))
                .thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches("oldpass", "hashedPassword"))
                .thenReturn(true);
        when(passwordEncoder.encode("newpass123"))
                .thenReturn("newHashedPassword");

        authService.changePassword(1L,
                new ChangePasswordRequest("oldpass", "newpass123"));

        assertEquals("newHashedPassword", testAccount.getPassword());
        assertEquals(1, testAccount.getTokenVersion());
        verify(userAccountRepository).save(testAccount);
    }

    @Test
    void changePassword_wrongOldPassword_throwsBadRequest() {
        when(userAccountRepository.findById(1L))
                .thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches("wrongold", "hashedPassword"))
                .thenReturn(false);

        AppException ex = assertThrows(AppException.class, () ->
                authService.changePassword(1L,
                        new ChangePasswordRequest("wrongold", "newpass123")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("WRONG_PASSWORD", ex.getCode());
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void changePassword_userNotFound_throwsNotFound() {
        when(userAccountRepository.findById(99L))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                authService.changePassword(99L,
                        new ChangePasswordRequest("old", "new123")));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void changePassword_incrementsTokenVersion_toInvalidatePreviousTokens() {
        when(userAccountRepository.findById(1L))
                .thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches("oldpass", "hashedPassword"))
                .thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("new");

        int versionBefore = testAccount.getTokenVersion();
        authService.changePassword(1L,
                new ChangePasswordRequest("oldpass", "newpass"));

        assertEquals(versionBefore + 1, testAccount.getTokenVersion());
    }

    // ── Get Current User ─────────────────────────────────────────────

    @Test
    void getCurrentUser_returnsUserInfo() {
        when(userAccountRepository.findActiveByIdWithRoles(1L))
                .thenReturn(Optional.of(testAccount));

        UserInfoResponse response = authService.getCurrentUser(1L);

        assertNotNull(response);
        assertEquals(1L, response.userId());
        assertEquals(10L, response.personId());
        assertEquals("Peter", response.fullName());
        assertEquals("01234567890", response.phone());
        assertFalse(response.roles().isEmpty());
    }

    @Test
    void getCurrentUser_notFound_throwsNotFound() {
        when(userAccountRepository.findActiveByIdWithRoles(99L))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                authService.getCurrentUser(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
