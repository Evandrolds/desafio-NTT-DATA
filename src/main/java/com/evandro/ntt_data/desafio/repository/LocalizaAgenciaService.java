package com.evandro.ntt_data.desafio.repository;

import com.evandro.ntt_data.desafio.dto.*;
import org.springframework.data.domain.Pageable;

public interface LocalizaAgenciaService {
    AgenciaResponse createAgencia(AgenciaRequest dto);
    PageResponse<DistanciaResponse> findClosestDistance(double latitude, double longitude, Double maxDistanceKm, int page, int size);
}
