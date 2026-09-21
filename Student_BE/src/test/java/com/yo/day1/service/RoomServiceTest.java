package com.yo.day1.service;

import com.yo.day1.domain.entity.Room;
import com.yo.day1.dto.room.RoomResponse;
import com.yo.day1.dto.room.RoonUpsertRequest;
import com.yo.day1.repository.RoomRepository;
import com.yo.day1.service.impl.RoomServiceImpl;
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
public class RoomServiceTest {

    @Mock
    private RoomRepository repository;

    @Mock
    private ModelMapper mapper;

    @InjectMocks
    private RoomServiceImpl service;

    // ==================== findAll ====================

    @Test
    void findAllReturnsList() {
        Room room = createEntity(1L);
        RoomResponse mockResponse = createMockResponse(1L);

        when(repository.findAll()).thenReturn(List.of(room));
        when(mapper.map(room, RoomResponse.class)).thenReturn(mockResponse);

        List<RoomResponse> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    // ==================== findById ====================

    @Test
    void findByIdSuccess() {
        Room room = createEntity(1L);
        RoomResponse mockResponse = createMockResponse(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(room));
        when(mapper.map(room, RoomResponse.class)).thenReturn(mockResponse);

        Optional<RoomResponse> result = service.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Optional<RoomResponse> result = service.findById(99L);

        assertThat(result).isEmpty();
    }

    // ==================== create / save ====================

    @Test
    void createSuccess() {
        RoonUpsertRequest request = buildRequest();
        Room room = createEntity(1L);
        RoomResponse mockResponse = createMockResponse(1L);

        when(mapper.map(request, Room.class)).thenReturn(new Room());
        when(repository.save(any(Room.class))).thenReturn(room);
        when(mapper.map(any(Room.class), eq(RoomResponse.class))).thenReturn(mockResponse);

        RoomResponse result = service.save(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    // ==================== update ====================

    @Test
    void updateSuccess() {
        RoonUpsertRequest request = buildRequest();
        Room existing = createEntity(1L);
        RoomResponse mockResponse = createMockResponse(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        doAnswer(invocation -> null)
                .when(mapper)
                .map(any(Object.class), any(Object.class));
        when(repository.save(any(Room.class))).thenReturn(existing);
        when(mapper.map(any(Room.class), eq(RoomResponse.class))).thenReturn(mockResponse);

        RoomResponse result = service.update(1L, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(mapper).map(request, existing);
    }

    @Test
    void updateThrowsWhenNotFound() {
        RoonUpsertRequest request = buildRequest();

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Room not found: 99");
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
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Room not found: 99");
        
        verify(repository, never()).deleteById(any());
    }

    // ==================== helpers ====================

    private RoonUpsertRequest buildRequest() {
        RoonUpsertRequest req = new RoonUpsertRequest();
        req.setRoomCode("LAB-101");
        req.setName("Phòng Lab 101");
        req.setCapacity(30);
        return req;
    }

    private RoomResponse createMockResponse(long id) {
        RoomResponse res = new RoomResponse();
        res.setId(id);
        return res;
    }

    private Room createEntity(long id) {
        Room entity = new Room();
        entity.setId(id);
        return entity;
    }
}
