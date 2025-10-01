package com.evandro.ntt_data.desafio.repository;

import com.evandro.ntt_data.desafio.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;

public interface LocalizaAgenciaService {
    AgenciaResponse createAgencia(AgenciaRequest dto);
    PageResponse<DistanciaResponse> findClosestDistance(double latitude, double longitude, Double maxDistanceKm, Integer page, Integer size);
}
