package com.yo.day1.service;

import com.yo.day1.common.exception.NotFoundExeception;
import com.yo.day1.domain.entity.Course;
import com.yo.day1.dto.course.CourseResponse;
import com.yo.day1.dto.course.CourseUpsertRequest;
import com.yo.day1.repository.CourseRepository;
import com.yo.day1.service.impl.CourseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CourseServiceTest {

    @Mock
    private CourseRepository repository;

    @Mock
    private ModelMapper mapper;

    @InjectMocks
    private CourseServiceImpl service;

    // ==================== findAll ====================

    @Test
    void findAllReturnsList() {
        Course course = createEntity(1L);
        CourseResponse mockResponse = createMockResponse(1L);

        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class))).thenReturn(List.of(course));
        when(mapper.map(course, CourseResponse.class)).thenReturn(mockResponse);

        List<CourseResponse> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void findAllWithSearchReturnsFilteredList() {
        Course course = createEntity(1L);
        CourseResponse mockResponse = createMockResponse(1L);

        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class))).thenReturn(List.of(course));
        when(mapper.map(course, CourseResponse.class)).thenReturn(mockResponse);

        List<CourseResponse> result = service.findAll("JAVA01");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    // ==================== findById ====================

    @Test
    void findByIdSuccess() {
        Course course = createEntity(1L);
        CourseResponse mockResponse = createMockResponse(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(course));
        when(mapper.map(course, CourseResponse.class)).thenReturn(mockResponse);

        Optional<CourseResponse> result = service.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Optional<CourseResponse> result = service.findById(99L);

        assertThat(result).isEmpty();
    }

    // ==================== create / save ====================

    @Test
    void createSuccess() {
        CourseUpsertRequest request = buildRequest();
        Course course = createEntity(1L);
        CourseResponse mockResponse = createMockResponse(1L);

        when(mapper.map(request, Course.class)).thenReturn(new Course());
        when(repository.save(any(Course.class))).thenReturn(course);
        when(mapper.map(any(Course.class), eq(CourseResponse.class))).thenReturn(mockResponse);

        CourseResponse result = service.save(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    // ==================== update ====================

    @Test
    void updateSuccess() {
        CourseUpsertRequest request = buildRequest();
        Course existing = createEntity(1L);
        CourseResponse mockResponse = createMockResponse(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        doAnswer(invocation -> null)
                .when(mapper)
                .map(any(Object.class), any(Object.class));
        when(repository.save(any(Course.class))).thenReturn(existing);
        when(mapper.map(any(Course.class), eq(CourseResponse.class))).thenReturn(mockResponse);

        CourseResponse result = service.update(1L, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(mapper).map(request, existing);
    }

    @Test
    void updateThrowsWhenNotFound() {
        CourseUpsertRequest request = buildRequest();

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request))
                .isInstanceOf(NotFoundExeception.class)
                .hasMessageContaining("Course not found: 99");
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
                .hasMessageContaining("Course not found: 99");
        
        verify(repository, never()).deleteById(any());
    }

    // ==================== helpers ====================

    private CourseUpsertRequest buildRequest() {
        CourseUpsertRequest req = new CourseUpsertRequest();
        req.setCourseCode("CRS-01");
        req.setCourseName("Toán 10");
        req.setTotalSession(30);
        return req;
    }

    private CourseResponse createMockResponse(long id) {
        CourseResponse res = new CourseResponse();
        res.setId(id);
        return res;
    }

    private Course createEntity(long id) {
        Course entity = new Course();
        entity.setId(id);
        return entity;
    }
}
