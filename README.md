Preciso que gere uma documentação das funcionalidades da minha API de localizar agencias bancária, no formato .md, para eu adicionar no github, segue os serviço: package com.evandro.ntt_data.desafio.controller;

import com.evandro.ntt_data.desafio.dto.*;
import com.evandro.ntt_data.desafio.repository.LocalizaAgenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agencia")
public class LocalizaAgenciaController {

    private final LocalizaAgenciaService service;

    @Autowired
    public LocalizaAgenciaController(LocalizaAgenciaService service){
        this.service = service;
    }
    @Operation(summary = "Cadastrar agencia")
    @PostMapping("/cadastrar")
    public ResponseEntity<AgenciaResponse> saveAgencia(@RequestBody(required = true) AgenciaRequest requestDTO){
      return new ResponseEntity<>(service.createAgencia(requestDTO),HttpStatus.CREATED);
    }
    @Operation(summary = "Buscar agencias próximas ordenadas por proximidade")
    @GetMapping("/agencias/closest")
    public ResponseEntity<PageResponse<DistanciaResponse>> findClosest(
            double latitude,
            double longitude,
            @RequestParam(required = false, defaultValue = "50") Double maxDistanceKm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<DistanciaResponse> result = service.findClosestDistance(latitude, longitude, maxDistanceKm,page,size);
        return ResponseEntity.ok(result);
    }

}, package com.evandro.ntt_data.desafio.service;

import com.evandro.ntt_data.desafio.domain.Agencia;
import com.evandro.ntt_data.desafio.dto.DistanciaResponse;
import com.evandro.ntt_data.desafio.dto.PageResponse;
import com.evandro.ntt_data.desafio.repository.LocalizaAgenciaRepository;
import com.evandro.ntt_data.desafio.util.CalcularDistancia;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AgenciaExternaService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final LocalizaAgenciaRepository repository;
    private final Logger logger = LoggerFactory.getLogger(AgenciaExternaService.class);

    public AgenciaExternaService(RestTemplate restTemplate, ObjectMapper objectMapper,
                                 LocalizaAgenciaRepository repository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.repository = repository;
    }

    public PageResponse<DistanciaResponse> findAgenciasBdOuApiExterna(
            double latitude,
            double longitude,
            Double maxDistanceKm,
            int page,
            int size) {

        // 1 - Busca no banco de dados
        List<DistanciaResponse> locais = repository.findAll().stream()
                .map(agencia -> {
                    Double distancia = CalcularDistancia.distanceKm(
                            latitude, longitude,
                            agencia.getLatitude(), agencia.getLongitude()
                    );
                    return new DistanciaResponse(
                            agencia.getId(),
                            agencia.getName(),
                            agencia.getLatitude(),
                            agencia.getLongitude(),
                            agencia.getAddress(),
                            distancia,
                            agencia.getExternalId()
                    );
                })
                .filter(resp -> resp.distanceKm() <= maxDistanceKm)
                .sorted(Comparator.comparingDouble(DistanciaResponse::distanceKm))
                .collect(Collectors.toList());

        if (!locais.isEmpty()) {
            logger.info("Agências encontradas no banco: {}", locais.size());
            return aplicarPaginacao(locais, page, size);
        }

        // 2 - Busca na API Overpass
        String jsonResponse = fetchOverpassResponse(latitude, longitude, maxDistanceKm);
        List<Agencia> externas = parseOverpassResponse(jsonResponse, latitude, longitude);

        // 3 - Mapeia para DTO
        List<DistanciaResponse> externasResp = externas.stream()
                .map(a -> new DistanciaResponse(
                        a.getId(),
                        a.getName(),
                        a.getLatitude(),
                        a.getLongitude(),
                        a.getAddress(),
                        a.getDistance(),
                        a.getExternalId()
                ))
                .sorted(Comparator.comparingDouble(DistanciaResponse::distanceKm))
                .collect(Collectors.toList());

        return aplicarPaginacao(externasResp, page, size);
    }

    private String fetchOverpassResponse(double lat, double lon, double raioKm) {
        String query = String.format(
                Locale.US,
                "[out:json];node[\"amenity\"=\"bank\"](around:%d,%.6f,%.6f);out;",
                (int)(raioKm * 1000), lat, lon
        );

        logger.info("Query Overpass: {}", query);

        String url = "https://overpass-api.de/api/interpreter";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("data", query);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            logger.info("Resposta Overpass recebida com sucesso!");
            return response.getBody();
        } else {
            throw new RuntimeException("Erro ao buscar dados no Overpass API: " + response.getStatusCode());
        }
    }

    private List<Agencia> parseOverpassResponse(String jsonResponse, double latitude, double longitude) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode elements = root.get("elements");
            List<Agencia> agencias = new ArrayList<>();

            if (elements != null && elements.isArray()) {
                for (JsonNode node : elements) {
                    if (!node.has("lat") || !node.has("lon")) continue;

                    Agencia agencia = new Agencia();

                    JsonNode tags = node.get("tags");
                    if (tags != null && tags.has("name")) {
                        agencia.setName(tags.get("name").asText());
                    } else {
                        agencia.setName("Agência Bancária");
                    }

                    StringBuilder endereco = new StringBuilder();
                    if (tags != null) {
                        if (tags.has("addr:street")) endereco.append(tags.get("addr:street").asText());
                        if (tags.has("addr:housenumber")) endereco.append(", ").append(tags.get("addr:housenumber").asText());
                        if (tags.has("addr:city")) endereco.append(" - ").append(tags.get("addr:city").asText());
                        if (tags.has("addr:state")) endereco.append("/").append(tags.get("addr:state").asText());
                    }

                    agencia.setAddress(endereco.toString());
                    agencia.setLatitude(node.get("lat").asDouble());
                    agencia.setLongitude(node.get("lon").asDouble());
                    agencia.setExternalId(node.get("id").asText());

                    double distancia = CalcularDistancia.distanceKm(latitude, longitude, agencia.getLatitude(), agencia.getLongitude());
                    agencia.setDistance(distancia);

                    agencias.add(agencia);
                }
            }

            logger.info("Encontradas {} agências no Overpass", agencias.size());
            return agencias;

        } catch (Exception e) {
            logger.error("Erro ao parsear resposta Overpass: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private PageResponse<DistanciaResponse> aplicarPaginacao(List<DistanciaResponse> lista, int page, int size) {
        int total = lista.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<DistanciaResponse> paged = lista.subList(fromIndex, toIndex);
        return new PageResponse<>(paged, page, size, total, 0, false);
    }
}
, package com.evandro.ntt_data.desafio.service;

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
, package com.evandro.ntt_data.desafio.repository;

import com.evandro.ntt_data.desafio.dto.*;
import org.springframework.data.domain.Pageable;

public interface LocalizaAgenciaService {
    AgenciaResponse createAgencia(AgenciaRequest dto);
    PageResponse<DistanciaResponse> findClosestDistance(double latitude, double longitude, Double maxDistanceKm, int page, int size);
}
,package com.evandro.ntt_data.desafio.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AgenciaRequest(

        @Size(min =2, max = 200)
        String name,
        @NotNull
        @DecimalMin(value = "-90.0", inclusive = true)
        @DecimalMax(value = "90.0", inclusive = true)
        Double latitude,
        @NotNull
        @DecimalMin(value = "-180.0", inclusive = true)
        @DecimalMax(value = "180.0", inclusive = true)
        Double longitude,
        String address,
        Double distancia

){}
,package com.evandro.ntt_data.desafio.dto;

import jakarta.validation.constraints.NotNull;

public record CoordenadasRequest(
        @NotNull
        Double latitude,
        @NotNull
        Double longitude,
        Double maxDistanceKm,
        Integer limit
){}
,public record DistanciaResponse(
        Long id,
        String name,
        Double positionX,
        Double positionY,
        String address,
        Double distanceKm,
        String externalId
){},package com.evandro.ntt_data.desafio.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}

,package com.evandro.ntt_data.desafio.dto;
public record AgenciaResponse(

        Long id,
        String name,
        Double positionX,
        Double positionY,
        String address,
        Double distacia){
}
,@Entity
@Table(name = "tb_agencia")
public class Agencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(nullable = false)
    private Double latitude;
    @Column(nullable = false)
    private Double longitude;
    private String address;
    @Column(name = "EXTERNALID")
    private String externalId;
    @Column(name = "DISTANCE")
    private Double distance;

    public Agencia(Long id, String name, Double latitude, Double longitude, String address, String externalId, Double distance) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.externalId = externalId;
        this.distance = distance;
    }
,package com.evandro.ntt_data.desafio.util;

public final class CalcularDistancia {
  private static final double raioDaTerraEmKm = 6371.0088;
  private CalcularDistancia(){}
     /**
     * Calcular a distância em quilômetros entre dois pontos (lat,lon) usando Haversine.
     */
    public static double distanceKm(double positionX, double positionY,double latitude, double longitude) {
        double latRad1 = Math.toRadians(positionX);
        double latRad2 = Math.toRadians(positionY);
        double deltaLat = Math.toRadians(latitude - positionX);
        double deltaLon = Math.toRadians(longitude - positionY);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(latRad1) * Math.cos(latRad2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return raioDaTerraEmKm * c;
    }
}
