package org.serviceproject.users.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.users.dto.AssignRolesRequest;
import org.serviceproject.users.dto.CreateUserRequest;
import org.serviceproject.users.dto.UpdateUserRequest;
import org.serviceproject.users.dto.UserResponse;
import org.serviceproject.users.entity.Gender;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.Role;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.entity.UserRole;
import org.serviceproject.users.repository.PersonRepository;
import org.serviceproject.users.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private Person person;
    private UserAccount account;

    @BeforeEach
    void setUp() {
        person = new Person();
        person.setId(10L);
        person.setFullName("مينا جرجس");
        person.setPhone("01001234567");
        person.setGender(Gender.MALE);
        person.setDateOfBirth(LocalDate.of(1995, 5, 20));

        account = new UserAccount();
        account.setId(1L);
        account.setPerson(person);
        account.setPassword("encodedPass");
        account.setEnabled(true);
        account.setTokenVersion(0);
    }

    @Test
    void findAll_returnsList() {
        when(userAccountRepository.findAll()).thenReturn(List.of(account));

        List<UserResponse> result = userService.findAll();

        assertEquals(1, result.size());
        assertEquals("مينا جرجس", result.get(0).fullName());
        assertEquals("01001234567", result.get(0).phone());
    }

    @Test
    void create_success() {
        CreateUserRequest request = new CreateUserRequest(
                "فادي نبيل",
                "01229876543",
                "pass123",
                Gender.MALE,
                LocalDate.of(1998, 1, 1),
                "Cairo",
                "أبونا داود"
        );

        when(personRepository.existsByPhone("01229876543")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("encoded");
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> {
            Person p = invocation.getArgument(0);
            p.setId(11L);
            return p;
        });
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount ua = invocation.getArgument(0);
            ua.setId(2L);
            return ua;
        });

        UserResponse response = userService.create(request);

        assertNotNull(response);
        assertEquals("فادي نبيل", response.fullName());
        assertEquals("01229876543", response.phone());
        verify(personRepository).save(any(Person.class));
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    @Test
    void create_duplicatePhone_throwsConflict() {
        CreateUserRequest request = new CreateUserRequest(
                "مينا",
                "01001234567",
                "pass123",
                Gender.MALE,
                null, null, null
        );

        when(personRepository.existsByPhone("01001234567")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> userService.create(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("PHONE_EXISTS", ex.getCode());
    }

    @Test
    void create_deletedPersonPhone_restoresAndEnablesAccount_success() {
        CreateUserRequest request = new CreateUserRequest(
                "مينا شنودة المستعاد",
                "01001234567",
                "newpass123",
                Gender.MALE,
                LocalDate.of(1995, 1, 1),
                "Cairo",
                "أبونا داود"
        );

        Person deletedPerson = new Person();
        deletedPerson.setId(101L);
        deletedPerson.setFullName("مينا شنودة القديم");
        deletedPerson.setPhone("01001234567");
        deletedPerson.softDelete();

        when(personRepository.existsByPhone("01001234567")).thenReturn(true);
        when(personRepository.findByPhone("01001234567")).thenReturn(Optional.of(deletedPerson));
        when(personRepository.save(any(Person.class))).thenAnswer(i -> i.getArgument(0));

        UserAccount existingAcc = new UserAccount();
        existingAcc.setId(501L);
        existingAcc.setPerson(deletedPerson);
        existingAcc.setEnabled(false);

        when(userAccountRepository.findByPersonId(101L)).thenReturn(Optional.of(existingAcc));
        when(passwordEncoder.encode("newpass123")).thenReturn("encoded-new");
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse response = userService.create(request);

        assertNotNull(response);
        assertEquals("مينا شنودة المستعاد", response.fullName());
        assertFalse(deletedPerson.isDeleted());
        assertTrue(existingAcc.isEnabled());
        verify(personRepository).save(deletedPerson);
        verify(userAccountRepository).save(existingAcc);
    }

    @Test
    void update_phoneChanged_incrementsTokenVersion() {
        UpdateUserRequest request = new UpdateUserRequest(
                "مينا جرجس بعد التعديل",
                "01099999999",
                Gender.MALE,
                LocalDate.of(1995, 5, 20),
                "Giza",
                "أبونا بولا"
        );

        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(personRepository.existsByPhone("01099999999")).thenReturn(false);

        UserResponse response = userService.update(1L, request);

        assertEquals("مينا جرجس بعد التعديل", response.fullName());
        assertEquals("01099999999", response.phone());
        assertEquals(1, account.getTokenVersion()); // token version incremented due to phone change
        verify(personRepository).save(person);
        verify(userAccountRepository).save(account);
    }

    @Test
    void assignRoles_success() {
        AssignRolesRequest request = new AssignRolesRequest(List.of(
                new AssignRolesRequest.RoleAssignment(Role.SERVICE_SECRETARY, 10L, null),
                new AssignRolesRequest.RoleAssignment(Role.SERVANT, null, null)
        ));

        when(userAccountRepository.findActiveByIdWithRoles(1L)).thenReturn(Optional.of(account));
        when(userAccountRepository.save(account)).thenReturn(account);

        UserResponse response = userService.assignRoles(1L, request);

        assertNotNull(response);
        assertEquals(2, account.getRoles().size());
        assertTrue(account.getRoles().stream().anyMatch(r -> r.getRole() == Role.SERVICE_SECRETARY));
        assertTrue(account.getRoles().stream().anyMatch(r -> r.getRole() == Role.SERVANT));
    }

    @Test
    void assignRoles_serviceSecretaryWithoutMinistry_throwsBadRequest() {
        AssignRolesRequest request = new AssignRolesRequest(List.of(
                new AssignRolesRequest.RoleAssignment(Role.SERVICE_SECRETARY, null, null)
        ));

        when(userAccountRepository.findActiveByIdWithRoles(1L)).thenReturn(Optional.of(account));

        AppException ex = assertThrows(AppException.class, () -> userService.assignRoles(1L, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("MINISTRY_REQUIRED", ex.getCode());
    }

    @Test
    void assignRoles_classSecretaryWithoutClass_throwsBadRequest() {
        AssignRolesRequest request = new AssignRolesRequest(List.of(
                new AssignRolesRequest.RoleAssignment(Role.CLASS_SECRETARY, null, null)
        ));

        when(userAccountRepository.findActiveByIdWithRoles(1L)).thenReturn(Optional.of(account));

        AppException ex = assertThrows(AppException.class, () -> userService.assignRoles(1L, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("CLASS_REQUIRED", ex.getCode());
    }

    @Test
    void toggleActive_disablesAndIncrementsTokenVersion() {
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        userService.toggleActive(1L);

        assertFalse(account.isEnabled());
        assertEquals(1, account.getTokenVersion());
        verify(userAccountRepository).save(account);
    }

    @Test
    void restoreDeleted_success() {
        person.softDelete();
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        userService.restoreDeleted(1L);

        assertFalse(person.isDeleted());
        verify(personRepository).save(person);
    }

    @Test
    void restoreDeleted_notDeleted_throwsBadRequest() {
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        AppException ex = assertThrows(AppException.class, () -> userService.restoreDeleted(1L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("NOT_DELETED", ex.getCode());
    }
}
