package com.yo.day1.service;

import com.yo.day1.common.exception.NotFoundExeception;
import com.yo.day1.domain.entity.Parent;
import com.yo.day1.domain.entity.Student;
import com.yo.day1.domain.enums.Gender;
import com.yo.day1.domain.enums.StudentStatus;
import com.yo.day1.dto.student.StudentResponse;
import com.yo.day1.dto.student.StudentUpsertRequest;
import com.yo.day1.repository.ParentRepository;
import com.yo.day1.repository.StudentRepository;
import com.yo.day1.repository.StudentStatusHistoryRepository;
import com.yo.day1.service.impl.StudentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private StudentStatusHistoryRepository statusHistoryRepository;

    @Mock
    private ModelMapper mapper;

    @InjectMocks
    private StudentServiceImpl studentService;

    // ==================== findByAll ====================

    @Test
    void findAllReturnsListOfResponses() {
        Student student = new Student();
        student.setId(1L);
        StudentResponse mockResponse = createMockResponse(1L, "SV01");

        when(studentRepository.findAll()).thenReturn(List.of(student));
        when(mapper.map(student, StudentResponse.class)).thenReturn(mockResponse);

        List<StudentResponse> result = studentService.findByAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    // ==================== findById ====================

    @Test
    void findByIdSuccess() {
        Student student = new Student();
        student.setId(1L);
        StudentResponse mockResponse = createMockResponse(1L, "SV01");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(mapper.map(student, StudentResponse.class)).thenReturn(mockResponse);

        Optional<StudentResponse> result = studentService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<StudentResponse> result = studentService.findById(99L);

        assertThat(result).isEmpty();
    }

    // ==================== create ====================

    @Test
    void createSuccess() {
        StudentUpsertRequest request = buildRequest(2L);
        Parent parent = new Parent();
        parent.setId(2L);
        StudentResponse mockResponse = createMockResponse(1L, "SV01");

        when(parentRepository.findById(2L)).thenReturn(Optional.of(parent));
        when(mapper.map(request, Student.class)).thenReturn(new Student());
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        when(mapper.map(any(Student.class), eq(StudentResponse.class))).thenReturn(mockResponse);

        StudentResponse result = studentService.create(request);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void createThrowsWhenParentNotFound() {
        StudentUpsertRequest request = buildRequest(99L);

        when(parentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.create(request))
                .isInstanceOf(NotFoundExeception.class)
                .hasMessageContaining("Phụ huynh không tồn tại: 99");
    }

    // ==================== update ====================

    @Test
    void updateSuccess() {
        StudentUpsertRequest request = buildRequest(2L);
        Parent parent = new Parent();
        parent.setId(2L);
        Student student = new Student();
        student.setId(1L);
        StudentResponse mockResponse = createMockResponse(1L, "SV01");
        doAnswer(invocation -> null)
                .when(mapper)
                .map(any(Object.class), any(Object.class));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(parentRepository.findById(2L)).thenReturn(Optional.of(parent));
        when(studentRepository.save(any(Student.class))).thenReturn(student);
        when(mapper.map(any(Student.class), eq(StudentResponse.class))).thenReturn(mockResponse);

        StudentResponse result = studentService.update(1L, request);

        assertThat(result.getId()).isEqualTo(1L);
        verify(mapper).map(request, student);
    }

    @Test
    void updateThrowsWhenStudentNotFound() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.update(1L, buildRequest(2L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Student not found: 1");
    }

    // ==================== getStudentForParent ====================

    @Test
    void getStudentForParentSuccess() {
        Parent parent = new Parent();
        parent.setId(5L);
        Student student = new Student();
        student.setId(1L);
        student.setParent(parent);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        Student result = studentService.getStudentForParent(1L, 5L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getStudentForParentThrowsAccessDenied() {
        Parent parent = new Parent();
        parent.setId(3L);
        Student student = new Student();
        student.setId(1L);
        student.setParent(parent);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> studentService.getStudentForParent(1L, 5L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Student does not belong to current parent account");
    }

    // ==================== search ====================

    @Test
    void searchReturnsPagedResponses() {
        Student student = new Student();
        student.setId(1L);
        Page<Student> page = new PageImpl<>(List.of(student));
        StudentResponse mockResponse = createMockResponse(1L, "SV01");

        when(studentRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class))).thenReturn(page);
        when(mapper.map(student, StudentResponse.class)).thenReturn(mockResponse);

        Page<StudentResponse> result = studentService.search(null, null, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
    }

    // ==================== helpers ====================

    private StudentUpsertRequest buildRequest(Long parentId) {
        StudentUpsertRequest req = new StudentUpsertRequest();
        req.setStudentCode("SV01");
        req.setFullName("Name");
        req.setDateOfBirth(LocalDate.now());
        req.setGender(Gender.MALE);
        req.setGradeLevel("10");
        req.setSchoolName("HS");
        req.setPhone("0123");
        req.setParentId(parentId);
        req.setStatus(StudentStatus.ACTIVE);
        req.setNote("Note");
        return req;
    }

    private StudentResponse createMockResponse(long id, String code) {
        StudentResponse res = new StudentResponse();
        res.setId(id);
        res.setStudentCode(code);
        res.setFullName("Name");
        res.setDateOfBirth(LocalDate.now());
        res.setGender(Gender.MALE);
        res.setGradeLevel("10");
        res.setSchoolName("HS");
        res.setPhone("0123");
        res.setStatus(StudentStatus.ACTIVE);
        res.setLatestScore(BigDecimal.ZERO);
        res.setNote("Note");
        return res;
    }
}