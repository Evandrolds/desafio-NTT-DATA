package com.evandro.ntt_data.desafio.service;

import com.evandro.ntt_data.desafio.domain.Agencia;
import com.evandro.ntt_data.desafio.dto.*;
import com.evandro.ntt_data.desafio.repository.LocalizaAgenciaRepository;
import com.evandro.ntt_data.desafio.repository.LocalizaAgenciaService;
import com.evandro.ntt_data.desafio.util.AgenciaMapper;
import io.micrometer.core.instrument.MeterRegistry;
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
    public PageResponse<DistanciaResponse> findClosestDistance(double latitude,
                                                               double longitude,
                                                               Double maxDistanceKm,
                                                               int page,
                                                               int size) {

        logger.info("Buscando agências mais próximas (lat={}, lon={}, raio={}km)", latitude, longitude, maxDistanceKm);

        // 1 - Tenta buscar localmente
        List<Agencia> agenciasBanco = buscarAgenciasDoBanco(latitude, longitude, maxDistanceKm, page, size);

        if (!agenciasBanco.isEmpty()) {
            logger.info("Agências encontradas no banco: {}", agenciasBanco.size());
            return mapper.toPageResponse(agenciasBanco);
        }

        // 2 - Busca na API externa
        PageResponse<DistanciaResponse> externas = externaService.findAgenciasBdOuApiExterna(latitude, longitude, maxDistanceKm, page, size);
        if(!externas.content().isEmpty()){
            String id = externas.content().get(0).externalId();
            if(!repository.existsById(Long.valueOf(id))){
                repository.saveAll(mapper.toListDistanciaResponse( externas));
            }
        }
        logger.info("Total de agências retornadas pela API externa: {}", externas.content().size());
        return externas;
    }

    private List<Agencia> buscarAgenciasDoBanco(double latitude, double longitude,
                                                double maxDistanceKm, Integer page, Integer size) {
        try {
            double latDelta = maxDistanceKm / 111.0;
            double lonDelta = maxDistanceKm / (111.320 * Math.cos(Math.toRadians(latitude)));

            Pageable pageable = PageRequest.of(page, size <= 0 ? DEFAULT_MAX_RESULTS : size, Sort.by("id"));

            return repository.findByLatitudeBetweenAndLongitudeBetween(
                    latitude - latDelta, latitude + latDelta,
                    longitude - lonDelta, longitude + lonDelta,
                    pageable
            ).getContent();

        } catch (Exception e) {
            logger.error("Erro ao buscar agências do banco: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
