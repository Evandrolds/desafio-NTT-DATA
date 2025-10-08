package com.evandro.ntt_data.desafio.service;

import com.evandro.ntt_data.desafio.domain.Agencia;
import com.evandro.ntt_data.desafio.dto.*;
import com.evandro.ntt_data.desafio.repository.LocalizaAgenciaRepository;
import com.evandro.ntt_data.desafio.repository.LocalizaAgenciaService;
import com.evandro.ntt_data.desafio.util.AgenciaMapper;
import com.evandro.ntt_data.desafio.util.CalcularDistancia;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LocalizaAgenciaServiceImpl implements LocalizaAgenciaService {

    private final LocalizaAgenciaRepository repository;
    private final AgenciaMapper mapper;
    private final MeterRegistry meterRegistry;
    private final AgenciaExternaService externaService;
    private final Logger logger = LoggerFactory.getLogger(LocalizaAgenciaServiceImpl.class);
    private static final int DEFAULT_MAX_RESULTS = 100;

    public LocalizaAgenciaServiceImpl(LocalizaAgenciaRepository repository,
                                      AgenciaMapper mapper,
                                      MeterRegistry meterRegistry,
                                      AgenciaExternaService externaService) {
        this.repository = repository;
        this.mapper = mapper;
        this.meterRegistry = meterRegistry;
        this.externaService = externaService;
    }

    @Override
    @Transactional
    public AgenciaResponse createAgencia(AgenciaRequest dto) {
        Agencia agencia = mapper.toEntity(dto);
        repository.save(agencia);
        logger.info("Agência criada id={} name={}", agencia.getId(), agencia.getName());
        return mapper.toAgenciaResponse(agencia);
    }

    @Override
    @Cacheable(value = "findClosest", key = "{#latitude, #longitude, #maxDistanceKm, #page, #size}")
    public PageResponse<AgenciaResponse>  encontrarAgenciasMaisProximas(double latitude,
                                                               double longitude,
                                                               double maxDistanceKm,
                                                               int page,
                                                               int size) {

        logger.info("🔍Buscando agências mais próximas (lat={}, lon={}, raio={}km)", latitude, longitude, maxDistanceKm);

        // 🔍 Tenta buscar localmente
        PageResponse<AgenciaResponse>  agenciasBanco = buscarAgenciasDoBanco(latitude, longitude, maxDistanceKm, page, size);
        logger.info("Total de agencias encontradas no bando local: ", agenciasBanco.content().size());
        if (!agenciasBanco.content().isEmpty()) {
            logger.info("Agências encontradas no banco: {}", agenciasBanco.content().size());
            return  agenciasBanco;
        }

        // 🔍 Busca na API externa (OverPass)
        PageResponse<AgenciaResponse> externas = externaService.encontrarAgenciasApiExterna(latitude, longitude, maxDistanceKm, page, size);

        if(!externas.content().isEmpty()){
            List<String> existeIDs = repository.findAllExternalIds();
             List<AgenciaResponse> novas = externas.content().stream().filter( id ->
                             id.externalId() != null && !existeIDs.contains(id.externalId())).toList();

             if(!novas.isEmpty()){
                 List<Agencia> entidades = mapper.toEntityList(novas);
                 repository.saveAll(entidades);
                 logger.info("💾 {} novas agências cadastradas no banco.", entidades.size());
             } else {
                 logger.info("✅ Nenhuma nova agência para cadastrar.");
             }

        }

        logger.info("✅Total de agências retornadas pela API externa: {}", externas.content().size());
        return externas;
    }
    private PageResponse<AgenciaResponse> buscarAgenciasDoBanco(
            double latitude,
            double longitude,
            double maxDistanceKm,
            Integer page,
            Integer size
    ) {
        try {
            // 1️⃣ Calcula os deltas para busca retangular (área aproximada)
            double latDelta = maxDistanceKm / 111.0;
            double lonDelta = maxDistanceKm / (111.320 * Math.cos(Math.toRadians(latitude)));

            Pageable pageable = PageRequest.of(
                    page,
                    (size == null || size <= 0) ? DEFAULT_MAX_RESULTS : size,
                    Sort.by("id")
            );

            // 2️⃣ Busca as agências no banco pela área retangular aproximada
            Page<Agencia> agenciasPage = repository.findByLatitudeBetweenAndLongitudeBetween(
                    latitude - latDelta, latitude + latDelta,
                    longitude - lonDelta, longitude + lonDelta,
                    pageable
            );

            // 3️⃣ Calcula a distância real e filtrar somente as que estão dentro do raio
            List<AgenciaResponse> agenciasComDistancia = agenciasPage.getContent().stream()
                    .map(agencia -> {
                        double distanciaReal = CalcularDistancia.distanceKm(
                                latitude,
                                longitude,
                                agencia.getLatitude(),
                                agencia.getLongitude()
                        );
                        agencia.setDistance(distanciaReal);

                        if (distanciaReal <= maxDistanceKm) {
                            return mapper.toAgenciaResponse(agencia);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingDouble(AgenciaResponse::distanceKm)) // ordena pela distância
                    .collect(Collectors.toList());

            // 4️⃣ Retorna o resultado paginado no formato do PageResponse
            logger.info("✅ Quantidade de agências encontradas no banco local: {} ",agenciasComDistancia.size());
            return mapper.toPageResponseFromList(agenciasComDistancia, pageable, agenciasPage.getTotalElements());

        } catch (Exception e) {
            logger.error("❌ Erro ao buscar agências do banco: {}", e.getMessage(), e);
            throw new EntityNotFoundException(
                    " ❌ Não foi possível buscar agências no banco devido ao erro: " + e.getMessage()
            );
        }
    }

}
