package com.yo.day1.service;

import com.yo.day1.common.exception.NotFoundExeception;
import com.yo.day1.domain.entity.ScheduleSlot;
import com.yo.day1.repository.ScheduleSlotRepository;
import com.yo.day1.service.impl.ScheduleSlotServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScheduleSlotServiceTest {

    @Mock
    private ScheduleSlotRepository repository;

    @InjectMocks
    private ScheduleSlotServiceImpl service;

    // ==================== findAll ====================

    @Test
    void findAllReturnsList() {
        ScheduleSlot slot = createEntity(1L);

        when(repository.findAll()).thenReturn(List.of(slot));

        List<ScheduleSlot> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    // ==================== findById ====================

    @Test
    void findByIdSuccess() {
        ScheduleSlot slot = createEntity(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(slot));

        Optional<ScheduleSlot> result = service.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Optional<ScheduleSlot> result = service.findById(99L);

        assertThat(result).isEmpty();
    }

    // ==================== save ====================

    @Test
    void saveSuccess() {
        ScheduleSlot slot = createEntity(1L);

        when(repository.save(any(ScheduleSlot.class))).thenReturn(slot);

        ScheduleSlot result = service.save(slot);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    // ==================== deleteById ====================

    @Test
    void deleteByIdSuccess() {
        when(repository.existsById(1L)).thenReturn(true);

        service.deleteById(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteByIdThrowsWhenNotFound() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteById(99L))
                .isInstanceOf(NotFoundExeception.class)
                .hasMessageContaining("ScheduleSlot not found: 99");
        
        verify(repository, never()).deleteById(any());
    }

    // ==================== helpers ====================

    private ScheduleSlot createEntity(long id) {
        ScheduleSlot entity = new ScheduleSlot();
        entity.setId(id);
        entity.setSlotCode("SLOT-01");
        entity.setWeekday((byte) 2);
        entity.setStartTime(java.time.LocalTime.of(17, 30));
        entity.setEndTime(java.time.LocalTime.of(19, 0));
        return entity;
    }
}
