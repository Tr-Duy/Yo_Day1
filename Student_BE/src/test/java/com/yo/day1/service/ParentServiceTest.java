package com.yo.day1.service;

import com.yo.day1.common.exception.NotFoundExeception;
import com.yo.day1.domain.entity.Parent;
import com.yo.day1.dto.parent.ParentResponse;
import com.yo.day1.dto.parent.ParentUpsertRequest;
import com.yo.day1.repository.ParentRepository;
import com.yo.day1.service.impl.ParentServiceImpl;
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
public class ParentServiceTest {

    @Mock
    private ParentRepository repository;

    @Mock
    private ModelMapper mapper;

    @InjectMocks
    private ParentServiceImpl service;

    // ==================== findAll ====================

    @Test
    void findAllReturnsList() {
        Parent parent = createEntity(1L);
        ParentResponse mockResponse = createMockResponse(1L);

        when(repository.findAll()).thenReturn(List.of(parent));
        when(mapper.map(parent, ParentResponse.class)).thenReturn(mockResponse);

        List<ParentResponse> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    // ==================== findById ====================

    @Test
    void findByIdSuccess() {
        Parent parent = createEntity(1L);
        ParentResponse mockResponse = createMockResponse(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(parent));
        when(mapper.map(parent, ParentResponse.class)).thenReturn(mockResponse);

        Optional<ParentResponse> result = service.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Optional<ParentResponse> result = service.findById(99L);

        assertThat(result).isEmpty();
    }

    // ==================== create / save ====================

    @Test
    void createSuccess() {
        ParentUpsertRequest request = buildRequest();
        Parent parent = createEntity(1L);
        ParentResponse mockResponse = createMockResponse(1L);

        when(mapper.map(request, Parent.class)).thenReturn(new Parent());
        when(repository.save(any(Parent.class))).thenReturn(parent);
        when(mapper.map(any(Parent.class), eq(ParentResponse.class))).thenReturn(mockResponse);

        ParentResponse result = service.save(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    // ==================== update ====================

    @Test
    void updateSuccess() {
        ParentUpsertRequest request = buildRequest();
        Parent existing = createEntity(1L);
        ParentResponse mockResponse = createMockResponse(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        doAnswer(invocation -> null)
                .when(mapper)
                .map(any(Object.class), any(Object.class));
        when(repository.save(any(Parent.class))).thenReturn(existing);
        when(mapper.map(any(Parent.class), eq(ParentResponse.class))).thenReturn(mockResponse);

        ParentResponse result = service.update(1L, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(mapper).map(request, existing);
    }

    @Test
    void updateThrowsWhenNotFound() {
        ParentUpsertRequest request = buildRequest();

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request))
                .isInstanceOf(NotFoundExeception.class)
                .hasMessageContaining("Parent not found: 99");
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
                .hasMessageContaining("Parent not found: 99");
        
        verify(repository, never()).deleteById(any());
    }

    // ==================== helpers ====================

    private ParentUpsertRequest buildRequest() {
        ParentUpsertRequest req = new ParentUpsertRequest();
        req.setFullName("Nguyễn Văn Phụ Huynh");
        return req;
    }

    private ParentResponse createMockResponse(long id) {
        ParentResponse res = new ParentResponse();
        res.setId(id);
        return res;
    }

    private Parent createEntity(long id) {
        Parent entity = new Parent();
        entity.setId(id);
        return entity;
    }
}
