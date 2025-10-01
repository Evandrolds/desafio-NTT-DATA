package com.evandro.ntt_data.desafio.service;

import com.evandro.ntt_data.desafio.domain.Agencia;
import com.evandro.ntt_data.desafio.dto.*;
import com.evandro.ntt_data.desafio.repository.LocalizaAgenciaRepository;
import com.evandro.ntt_data.desafio.testConfig.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@SpringBootTest
@Import(TestConfig.class) // adiciona o SimpleMeterRegistry no contexto de teste
class LocalizaAgenciaServiceImplTest {

    @Autowired
    private LocalizaAgenciaServiceImpl service;

    @MockitoBean
    private LocalizaAgenciaRepository repository;

    @Test
    void createAgenciaTestPersistReturn() {
        AgenciaRequest dto = new AgenciaRequest("Ponto A", -23.55, -46.63, "Rua X");
        Agencia agencia = new Agencia(1L, dto.name(), dto.latitude(), dto.longitude(), dto.address());

        when(repository.save(any(Agencia.class))).thenAnswer(invocation -> {
            Agencia a = invocation.getArgument(0);
            a.setId(1L);
            return a;
        });

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

        Page<Agencia> page = new PageImpl<>(List.of(p1, p2));

        when(repository.findByLatitudeBetweenAndLongitudeBetween(
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(Pageable.class)))
                .thenReturn(page);

        CoordenadasRequest requestDTO = new CoordenadasRequest(
                -23.5500, -46.6330, null, null
        );

        PageResponse<DistanciaResponse> result =
                service.findClosestDistance(
                        requestDTO.latitude(),
                        requestDTO.longitude(),
                        requestDTO.maxDistanceKm(),
                        0, 10
                );

        assertEquals(2, result.content().size());
        assertTrue(result.content().get(0).distanceKm() <= result.content().get(1).distanceKm());
    }

}
