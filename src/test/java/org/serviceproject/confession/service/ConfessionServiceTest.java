package org.serviceproject.confession.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.academic.service.AcademicYearService;
import org.serviceproject.classes.entity.GradeClass;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.RoleWithScope;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.confession.dto.ConfessionResponse;
import org.serviceproject.confession.dto.ConfessionSessionDto;
import org.serviceproject.confession.dto.CreateConfessionRequest;
import org.serviceproject.confession.dto.CreateConfessionSessionRequest;
import org.serviceproject.confession.dto.StudentConfessionSummaryDto;
import org.serviceproject.confession.dto.UpdateConfessionRequest;
import org.serviceproject.confession.entity.ConfessionRecord;
import org.serviceproject.confession.repository.ConfessionRecordRepository;
import org.serviceproject.ministries.entity.Ministry;
import org.serviceproject.students.entity.StudentPlacement;
import org.serviceproject.students.entity.StudentStatus;
import org.serviceproject.students.repository.StudentPlacementRepository;
import org.serviceproject.users.entity.Gender;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.Role;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.repository.PersonRepository;
import org.serviceproject.users.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfessionServiceTest {

    @Mock
    private ConfessionRecordRepository confessionRecordRepository;

    @Mock
    private StudentPlacementRepository studentPlacementRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private AcademicYearService academicYearService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private ConfessionService confessionService;

    private AcademicYear academicYear;
    private Person student;
    private Person servant;
    private UserAccount servantAccount;
    private StudentPlacement studentPlacement;
    private ConfessionRecord confessionRecord;

    private UserPrincipal servantPrincipal;
    private UserPrincipal otherServantPrincipal;

    @BeforeEach
    void setUp() {
        academicYear = new AcademicYear("2026/2027", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 8, 31), true);
        academicYear.setId(1L);

        student = new Person();
        student.setId(5001L);
        student.setFullName("مارك سامح");
        student.setPhone("01111111111");
        student.setGender(Gender.MALE);
        student.setConfessionFather("أبونا بيشوي");

        servant = new Person();
        servant.setId(3001L);
        servant.setFullName("مينا جرجس");
        servant.setPhone("01001111111");
        servant.setGender(Gender.MALE);

        servantAccount = new UserAccount();
        servantAccount.setId(201L);
        servantAccount.setPerson(servant);

        Ministry ministry = new Ministry("ابتدائي");
        ministry.setId(1000L);

        GradeClass gradeClass = new GradeClass("أولى ابتدائي", ministry);
        gradeClass.setId(2000L);

        studentPlacement = new StudentPlacement(student, academicYear, ministry, gradeClass);
        studentPlacement.assignServant(servant);

        confessionRecord = new ConfessionRecord(
                student, academicYear, LocalDate.of(2026, 9, 15),
                "أبونا بيشوي", "اعتراف دوري", servantAccount
        );
        confessionRecord.setId(701L);

        servantPrincipal = new UserPrincipal(201L, 3001L, "01001111111", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVANT, null, null)));

        otherServantPrincipal = new UserPrincipal(301L, 3002L, "01002222222", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVANT, null, null)));
    }

    @Test
    void create_success_inheritsConfessionFather() {
        CreateConfessionRequest request = new CreateConfessionRequest(
                5001L, LocalDate.of(2026, 9, 15), null, "ملاحظات"
        );

        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5001L, 1L))
                .thenReturn(Optional.of(studentPlacement));
        when(confessionRecordRepository.existsByStudentIdAndConfessionDate(5001L, LocalDate.of(2026, 9, 15)))
                .thenReturn(false);
        when(userAccountRepository.findById(201L)).thenReturn(Optional.of(servantAccount));

        when(confessionRecordRepository.save(any(ConfessionRecord.class))).thenAnswer(i -> {
            ConfessionRecord cr = i.getArgument(0);
            cr.setId(702L);
            return cr;
        });

        ConfessionResponse response = confessionService.create(request, servantPrincipal);

        assertNotNull(response);
        assertEquals("أبونا بيشوي", response.confessionFather()); // inherited from student profile
        assertEquals(LocalDate.of(2026, 9, 15), response.confessionDate());
    }

    @Test
    void create_duplicateDate_throwsConflict() {
        CreateConfessionRequest request = new CreateConfessionRequest(
                5001L, LocalDate.of(2026, 9, 15), "أبونا بيشوي", null
        );

        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5001L, 1L))
                .thenReturn(Optional.of(studentPlacement));
        when(confessionRecordRepository.existsByStudentIdAndConfessionDate(5001L, LocalDate.of(2026, 9, 15)))
                .thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> confessionService.create(request, servantPrincipal));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("CONFESSION_EXISTS", ex.getCode());
    }

    @Test
    void create_outOfScope_throwsForbidden() {
        CreateConfessionRequest request = new CreateConfessionRequest(
                5001L, LocalDate.of(2026, 9, 15), "أبونا بيشوي", null
        );

        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5001L, 1L))
                .thenReturn(Optional.of(studentPlacement));

        AppException ex = assertThrows(AppException.class, () -> confessionService.create(request, otherServantPrincipal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    @Test
    void findByStudent_success() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5001L, 1L))
                .thenReturn(Optional.of(studentPlacement));
        when(confessionRecordRepository.findAllByStudentId(5001L)).thenReturn(List.of(confessionRecord));

        List<ConfessionResponse> list = confessionService.findByStudent(5001L, servantPrincipal);

        assertEquals(1, list.size());
        assertEquals(701L, list.get(0).id());
        assertEquals("مارك سامح", list.get(0).studentName());
    }

    @Test
    void delete_success() {
        when(confessionRecordRepository.findByIdWithDetails(701L)).thenReturn(Optional.of(confessionRecord));
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5001L, 1L))
                .thenReturn(Optional.of(studentPlacement));

        confessionService.delete(701L, servantPrincipal);

        verify(confessionRecordRepository).delete(confessionRecord);
    }

    @Test
    void getOverview_servant_success() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findAllByAcademicYearIdAndServantIdAndStatus(1L, 3001L, StudentStatus.ACTIVE))
                .thenReturn(List.of(studentPlacement));
        when(confessionRecordRepository.findAllByAcademicYearId(1L)).thenReturn(List.of(confessionRecord));

        List<StudentConfessionSummaryDto> list = confessionService.getOverview(servantPrincipal);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("مارك سامح", list.get(0).studentName());
        assertEquals("أولى ابتدائي", list.get(0).className());
        assertEquals("مينا جرجس", list.get(0).servantName());
        assertEquals(1, list.get(0).totalConfessionsThisYear());
    }

    @Test
    void createSession_success() {
        CreateConfessionSessionRequest sessionRequest = new CreateConfessionSessionRequest(
                LocalDate.of(2026, 9, 20),
                "أبونا بولا",
                List.of(5001L),
                "جلسة "
        );

        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(userAccountRepository.findById(201L)).thenReturn(Optional.of(servantAccount));
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5001L, 1L)).thenReturn(Optional.of(studentPlacement));
        when(confessionRecordRepository.findByStudentIdAndConfessionDate(5001L, LocalDate.of(2026, 9, 20)))
                .thenReturn(Optional.empty());
        when(confessionRecordRepository.save(any(ConfessionRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfessionSessionDto dto = confessionService.createSession(sessionRequest, servantPrincipal);

        assertNotNull(dto);
        assertEquals("أبونا بولا", dto.confessionFather());
        assertEquals(LocalDate.of(2026, 9, 20), dto.sessionDate());
        assertEquals(1, dto.studentCount());
        assertEquals("جلسة", dto.notes());
        assertEquals(1, dto.students().size());
        assertEquals(5001L, dto.students().get(0).studentId());
        assertEquals("أبونا بولا", student.getConfessionFather());
    }

    @Test
    void getSessions_servant_success() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findAllByAcademicYearIdAndServantIdAndStatus(1L, 3001L, StudentStatus.ACTIVE))
                .thenReturn(List.of(studentPlacement));
        when(confessionRecordRepository.findAllByAcademicYearId(1L)).thenReturn(List.of(confessionRecord));

        List<ConfessionSessionDto> sessions = confessionService.getSessions(servantPrincipal);

        assertNotNull(sessions);
        assertEquals(1, sessions.size());
        assertEquals("أبونا بيشوي", sessions.get(0).confessionFather());
        assertEquals(1, sessions.get(0).studentCount());
        assertEquals("مارك سامح", sessions.get(0).students().get(0).studentName());
    }
}
