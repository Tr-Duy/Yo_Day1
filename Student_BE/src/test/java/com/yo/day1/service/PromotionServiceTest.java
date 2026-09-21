package com.yo.day1.service;

import com.yo.day1.common.exception.NotFoundExeception;
import com.yo.day1.domain.entity.Promotion;
import com.yo.day1.dto.promotion.PromotionResponse;
import com.yo.day1.dto.promotion.PromotionUpsertRequest;
import com.yo.day1.repository.PromotionRepository;
import com.yo.day1.service.impl.PromotionServiceImpl;
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
public class PromotionServiceTest {

    @Mock
    private PromotionRepository repository;

    @Mock
    private ModelMapper mapper;

    @InjectMocks
    private PromotionServiceImpl service;

    // ==================== findAll ====================

    @Test
    void findAllReturnsList() {
        Promotion promotion = createEntity(1L);
        PromotionResponse mockResponse = createMockResponse(1L);

        when(repository.findAll()).thenReturn(List.of(promotion));
        when(mapper.map(promotion, PromotionResponse.class)).thenReturn(mockResponse);

        List<PromotionResponse> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    // ==================== findById ====================

    @Test
    void findByIdSuccess() {
        Promotion promotion = createEntity(1L);
        PromotionResponse mockResponse = createMockResponse(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(promotion));
        when(mapper.map(promotion, PromotionResponse.class)).thenReturn(mockResponse);

        Optional<PromotionResponse> result = service.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Optional<PromotionResponse> result = service.findById(99L);

        assertThat(result).isEmpty();
    }

    // ==================== create / save ====================

    @Test
    void createSuccess() {
        PromotionUpsertRequest request = buildRequest();
        Promotion promotion = createEntity(1L);
        PromotionResponse mockResponse = createMockResponse(1L);

        when(mapper.map(request, Promotion.class)).thenReturn(new Promotion());
        when(repository.save(any(Promotion.class))).thenReturn(promotion);
        when(mapper.map(any(Promotion.class), eq(PromotionResponse.class))).thenReturn(mockResponse);

        PromotionResponse result = service.save(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    // ==================== update ====================

    @Test
    void updateSuccess() {
        PromotionUpsertRequest request = buildRequest();
        Promotion existing = createEntity(1L);
        PromotionResponse mockResponse = createMockResponse(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        doAnswer(invocation -> null)
                .when(mapper)
                .map(any(Object.class), any(Object.class));
        when(repository.save(any(Promotion.class))).thenReturn(existing);
        when(mapper.map(any(Promotion.class), eq(PromotionResponse.class))).thenReturn(mockResponse);

        PromotionResponse result = service.update(1L, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(mapper).map(request, existing);
    }

    @Test
    void updateThrowsWhenNotFound() {
        PromotionUpsertRequest request = buildRequest();

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request))
                .isInstanceOf(NotFoundExeception.class)
                .hasMessageContaining("Promotion not found: 99");
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
                .hasMessageContaining("Promotion not found: 99");
        
        verify(repository, never()).deleteById(any());
    }

    // ==================== helpers ====================

    private PromotionUpsertRequest buildRequest() {
        PromotionUpsertRequest req = new PromotionUpsertRequest();
        req.setPromoCode("SUMMER2026");
        req.setName("Khuyến mãi hè");
        return req;
    }

    private PromotionResponse createMockResponse(long id) {
        PromotionResponse res = new PromotionResponse();
        res.setId(id);
        return res;
    }

    private Promotion createEntity(long id) {
        Promotion entity = new Promotion();
        entity.setId(id);
        return entity;
    }
}
