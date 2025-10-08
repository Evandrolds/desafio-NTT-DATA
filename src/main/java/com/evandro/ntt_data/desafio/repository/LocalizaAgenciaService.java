package com.evandro.ntt_data.desafio.repository;

import com.evandro.ntt_data.desafio.dto.*;

import java.util.List;

public interface LocalizaAgenciaService {
    AgenciaResponse createAgencia(AgenciaRequest dto);
    PageResponse<AgenciaResponse>  encontrarAgenciasMaisProximas(double latitude, double longitude, double maxDistanceKm, int page, int size);
}
