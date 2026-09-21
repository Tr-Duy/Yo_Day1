package com.yo.day1.service;

import com.yo.day1.common.exception.NotFoundExeception;
import com.yo.day1.domain.entity.Teacher;
import com.yo.day1.domain.enums.TeacherRole;
import com.yo.day1.domain.enums.TeacherStatus;
import com.yo.day1.dto.teacher.TeacherResponse;
import com.yo.day1.dto.teacher.TeacherUpsertRequest;
import com.yo.day1.repository.TeacherRepository;
import com.yo.day1.service.impl.TeacherServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TeacherServiceTest {

    @Mock
    private TeacherRepository repository;

    @Mock
    private ModelMapper mapper;

    @InjectMocks
    private TeacherServiceImpl service;

    // ==================== search ====================

    @Test
    @SuppressWarnings("unchecked")
    void searchReturnsPagedResult() {
        Teacher teacher = createEntity(1L);
        TeacherResponse mockResponse = createMockResponse(1L);
        Page<Teacher> page = new PageImpl<>(List.of(teacher));

        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(mapper.map(teacher, TeacherResponse.class)).thenReturn(mockResponse);

        Page<TeacherResponse> result = service.search("John", TeacherStatus.ACTIVE, TeacherRole.TEACHER, true, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
    }

    // ==================== findAll ====================

    @Test
    void findAllReturnsListWhenActiveIsNull() {
        Teacher teacher = createEntity(1L);
        TeacherResponse mockResponse = createMockResponse(1L);

        when(repository.findAll()).thenReturn(List.of(teacher));
        when(mapper.map(teacher, TeacherResponse.class)).thenReturn(mockResponse);

        List<TeacherResponse> result = service.findAll(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void findAllReturnsListWhenActiveIsTrue() {
        Teacher teacher = createEntity(1L);
        TeacherResponse mockResponse = createMockResponse(1L);

        when(repository.findByIsActive(true)).thenReturn(List.of(teacher));
        when(mapper.map(teacher, TeacherResponse.class)).thenReturn(mockResponse);

        List<TeacherResponse> result = service.findAll(true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    // ==================== findById ====================

    @Test
    void findByIdSuccess() {
        Teacher teacher = createEntity(1L);
        TeacherResponse mockResponse = createMockResponse(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(teacher));
        when(mapper.map(teacher, TeacherResponse.class)).thenReturn(mockResponse);

        Optional<TeacherResponse> result = service.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Optional<TeacherResponse> result = service.findById(99L);

        assertThat(result).isEmpty();
    }

    // ==================== create / save ====================

    @Test
    void createSuccess() {
        TeacherUpsertRequest request = buildRequest();
        Teacher teacher = createEntity(1L);
        TeacherResponse mockResponse = createMockResponse(1L);

        when(mapper.map(request, Teacher.class)).thenReturn(new Teacher());
        when(repository.save(any(Teacher.class))).thenReturn(teacher);
        when(mapper.map(any(Teacher.class), eq(TeacherResponse.class))).thenReturn(mockResponse);

        TeacherResponse result = service.save(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    // ==================== update ====================

    @Test
    void updateSuccess() {
        TeacherUpsertRequest request = buildRequest();
        Teacher existing = createEntity(1L);
        TeacherResponse mockResponse = createMockResponse(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        doAnswer(invocation -> null)
                .when(mapper)
                .map(any(Object.class), any(Object.class));
        when(repository.save(any(Teacher.class))).thenReturn(existing);
        when(mapper.map(any(Teacher.class), eq(TeacherResponse.class))).thenReturn(mockResponse);

        TeacherResponse result = service.update(1L, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(mapper).map(request, existing);
    }

    @Test
    void updateThrowsWhenNotFound() {
        TeacherUpsertRequest request = buildRequest();

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request))
                .isInstanceOf(NotFoundExeception.class)
                .hasMessageContaining("Teacher not found: 99");
    }

    // ==================== delete ====================

    @Test
    void deleteSuccess() {
        when(repository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteThrowsWhenNotFound() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(NotFoundExeception.class)
                .hasMessageContaining("Teacher not found: 99");
        
        verify(repository, never()).deleteById(any());
    }

    // ==================== helpers ====================

    private TeacherUpsertRequest buildRequest() {
        TeacherUpsertRequest req = new TeacherUpsertRequest();
        req.setTeacherCode("GV-01");
        req.setFullName("Nguyễn Văn Giáo Viên");
        req.setPhone("0912345678");
        return req;
    }

    private TeacherResponse createMockResponse(long id) {
        TeacherResponse res = new TeacherResponse();
        res.setId(id);
        return res;
    }

    private Teacher createEntity(long id) {
        Teacher entity = new Teacher();
        entity.setId(id);
        return entity;
    }
}
