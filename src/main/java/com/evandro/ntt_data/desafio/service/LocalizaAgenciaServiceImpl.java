package com.evandro.ntt_data.desafio.service;

import com.evandro.ntt_data.desafio.domain.Agencia;
import com.evandro.ntt_data.desafio.dto.*;
import com.evandro.ntt_data.desafio.repository.LocalizaAgenciaRepository;
import com.evandro.ntt_data.desafio.repository.LocalizaAgenciaService;
import com.evandro.ntt_data.desafio.util.AgenciaMapper;
import com.evandro.ntt_data.desafio.util.CalcularDistancia;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class LocalizaAgenciaServiceImpl implements LocalizaAgenciaService {

    private final LocalizaAgenciaRepository repository;
    private final AgenciaMapper mapper;
    private final MeterRegistry meterRegistry;
    private final Logger logger = LoggerFactory.getLogger(LocalizaAgenciaServiceImpl.class);
    private final AgenciaExternaService externaService;
    private final static int DEFAULT_MAX_RESULTS = 100;

    @Autowired
    public LocalizaAgenciaServiceImpl(LocalizaAgenciaRepository repository,
                                      AgenciaMapper mapper, MeterRegistry meterRegistry,
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
        logger.info("Agencia criada id={} name={}", agencia.getId(), agencia.getName());
        return mapper.toAgenciaResponse(agencia);


    }

    @Override
    @Transactional
    @Cacheable(value = "findClosest", key = "T(java.lang.String).format('%s:%s:%s:%s:%s', #latitude, #longitude, #maxDistanceKm, #page, #size)")
    public PageResponse<DistanciaResponse> findClosestDistance(double latitude, double longitude, Double maxDistanceKm, Integer page, Integer size) {

        Timer.Sample sample = Timer.start(meterRegistry);

        double effectiveKm = maxDistanceKm == null ? 50.0 : maxDistanceKm;

        double latDelta = effectiveKm / 111.0;
        double lonDelta = effectiveKm / (111.320 * Math.cos(Math.toRadians(latitude)));
        double minLat = latitude - latDelta;
        double maxLat = latitude + latDelta;
        double minLon = longitude - lonDelta;
        double maxLon = longitude + lonDelta;

        Pageable pageable = PageRequest.of(page, size <= 0 ? DEFAULT_MAX_RESULTS : size, Sort.by("id"));

        // Aqui você já deve aplicar o filtro bounding-box no repository
        Page<Agencia> agencia = repository.findByLatitudeBetweenAndLongitudeBetween(minLat, maxLat, minLon, maxLon, pageable);
        List<Agencia> agenciaExterna = externaService.buscarAgenciasProximas(latitude,longitude,maxDistanceKm != null?  maxDistanceKm: 50.0);

        List<Agencia> agencias = Stream.concat(
                agencia.stream(),
                agenciaExterna.stream()
        ).collect(Collectors.toList());

        List<DistanciaResponse> filtered = agencias.stream()
                .map(p -> {
                    double dist = CalcularDistancia.distanceKm(latitude, longitude, p.getLatitude(), p.getLongitude());
                    return mapper.toDistanciaResponse(p, dist);
                })
                .filter(d -> maxDistanceKm == null || d.distanceKm() <= maxDistanceKm)
                .sorted(Comparator.comparingDouble(DistanciaResponse::distanceKm))
                .toList();

        Page<DistanciaResponse> pageImpl = new PageImpl<>(filtered, pageable, filtered.size());

        sample.stop(meterRegistry.timer("agencia.findClosestDistance.timer"));

        logger.debug("findClosest center=({},{}) maxKm={} candidates={} returned={}",
                latitude, longitude, maxDistanceKm, agencia.getTotalElements(), pageImpl.getNumberOfElements());

        return PageResponse.of(pageImpl);
    }

}