package com.evandro.ntt_data.desafio;

import com.evandro.ntt_data.desafio.domain.Agencia;
import com.evandro.ntt_data.desafio.dto.*;
import com.evandro.ntt_data.desafio.repository.LocalizaAgenciaRepository;
import com.evandro.ntt_data.desafio.service.LocalizaAgenciaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LocalizaAgenciaServiceImplTest {

    @Mock
    private LocalizaAgenciaRepository repository;
    @InjectMocks
    private LocalizaAgenciaServiceImpl service;
    @BeforeEach
    public void setUp(){
        MockitoAnnotations.openMocks(this);
    }
    @Test
    public void createAgenciaTestPersistReturnDto(){
        AgenciaRequest dto = new AgenciaRequest(
                "Ponto A",
                -23.55,
                -46.63,
                "Rua X"
                );
        Agencia agencia = new Agencia(
                1L,
                dto.name(),
                dto.latitude(),
                dto.longitude(),
                dto.address());

        when(repository.save(any(Agencia.class))).thenReturn(agencia);

        AgenciaResponse responseDTO = service.createAgencia(dto);
        assertNotNull(responseDTO);
        assertEquals(1L, responseDTO.id());
        assertEquals("Ponto A", responseDTO.name());
        verify(repository, times(1)).save(any(Agencia.class));
    }
    @Test
    void findNearOrAgenceDistance() {
        Agencia p1 = new Agencia(1L,"A",-23.5505,-46.6333,"A");
        Agencia p2 = new Agencia(2L,"B",-23.5505,-46.6333,"B");
        CoordenadasRequest requestDTO = new CoordenadasRequest(
                -23.5500, -46.6330,null,null
        );
        when(repository.findAll()).thenReturn(List.of(p1, p2));

        PageResponse<DistanciaResponse> result = service.findClosestDistance(requestDTO.latitude(),requestDTO.longitude(),requestDTO.maxDistanceKm(),null,null);

        assertEquals(2, result.content().size());
        assertTrue(result.content().get(0).distanceKm() <= result.content().get(1).distanceKm());
    }
}
