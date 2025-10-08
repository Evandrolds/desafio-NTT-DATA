package com.evandro.ntt_data.desafio.service;

import com.evandro.ntt_data.desafio.config.ApiExternaConfiguration;
import com.evandro.ntt_data.desafio.domain.Agencia;
import com.evandro.ntt_data.desafio.dto.AgenciaResponse;
import com.evandro.ntt_data.desafio.dto.PageResponse;
import com.evandro.ntt_data.desafio.repository.LocalizaAgenciaRepository;
import com.evandro.ntt_data.desafio.util.AgenciaMapper;
import com.evandro.ntt_data.desafio.util.CalcularDistancia;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final ApiExternaConfiguration configuration;
    private final AgenciaMapper mapper;
    private final Logger logger = LoggerFactory.getLogger(AgenciaExternaService.class);

    public AgenciaExternaService(RestTemplate restTemplate,
                                 ObjectMapper objectMapper,
                                 LocalizaAgenciaRepository repository,
                                 ApiExternaConfiguration configuration,
                                 AgenciaMapper mapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.configuration = configuration;
        this.mapper = mapper;
    }

    // 🚀 Aplica resiliência nas chamadas externas
    @Retry(name = "overpassRetry")
    @CircuitBreaker(name = "overpassCircuitBreaker", fallbackMethod = "fallbackBuscarAgencias")
//    @TimeLimiter(name = "overpassTimeout")
    public PageResponse<AgenciaResponse> encontrarAgenciasApiExterna(
            double latitude,
            double longitude,
            Double maxDistanceKm,
            int page,
            int size) {

        // 🔍 Busca na API Overpass (Externa)
        String jsonResponse = fetchOverpassResponse(latitude, longitude, maxDistanceKm);
        List<AgenciaResponse> agenciasExternas = parseOverpassResponse(jsonResponse, latitude, longitude).content().stream().toList();

        logger.info("🌎 Agências retornadas da Overpass: {}", agenciasExternas.size());
        return aplicarPaginacao(agenciasExternas,page,size);
    }

    private String fetchOverpassResponse(double lat, double lon, double raioKm) {

        String query = configuration.getReturnQuery(raioKm,lat,lon);

        logger.info("📡 Query Overpass: {}", query);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("data", query);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                configuration.getApi_Url(),
                HttpMethod.POST,
                request,
                String.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            logger.info("✅ Resposta Overpass recebida com sucesso!");
            return response.getBody();
        } else {
            throw new RuntimeException("Erro ao buscar dados no Overpass API: " + response.getStatusCode());
        }
    }

    private PageResponse<AgenciaResponse> parseOverpassResponse(String jsonResponse, double latitude, double longitude) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode elements = root.get("elements");
            List<AgenciaResponse> agencias = new ArrayList<>();

            if (elements != null && elements.isArray()) {
                for (JsonNode node : elements) {
                    if (!node.has("lat") || !node.has("lon")) continue;

                    Agencia agencia = new Agencia();
                    JsonNode tags = node.get("tags");

                    agencia.setName(tags != null && tags.has("name")
                            ? tags.get("name").asText()
                            : "Agência Bancária");

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

                    double distancia = CalcularDistancia.distanceKm(latitude, longitude,
                            agencia.getLatitude(), agencia.getLongitude());
                    agencia.setDistance(distancia);

                    agencias.add(mapper.toAgenciaResponse(agencia));
                }
            }

            logger.info("📍 Encontradas {} agências no Overpass", agencias.size());

            // ✅ Cria um Pageable padrão (primeira página com todos os itens, caso não venha nada)
            Pageable pageable = PageRequest.of(0, agencias.isEmpty() ? 10 : agencias.size());


            return mapper.toPageResponseFromList(agencias, pageable, agencias.size());

        } catch (Exception e) {
            logger.error("Erro ao parsear resposta Overpass: {}", e.getMessage());
            return new PageResponse<>(
                    List.of(),
                    0,
                    0,
                    0,
                    true
            );
        }
    }

    private PageResponse<AgenciaResponse> aplicarPaginacao(List<AgenciaResponse> lista, int page, int size) {
        if (lista == null) {
            return null;
        }
        int total = lista.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, total);

        List<AgenciaResponse> content = lista.subList(fromIndex, toIndex);

        // Cria um PageImpl manualmente
        Pageable pageable = PageRequest.of(page, size);
        Page<AgenciaResponse> pageImpl = new PageImpl<>(content, pageable, total);

        return PageResponse.of(pageImpl);
    }
    // 🧱 Fallback - chamado se o Overpass falhar (timeout, indisponível, etc)
    private PageResponse<AgenciaResponse> fallbackBuscarAgencias(
            double latitude,
            double longitude,
            Double maxDistanceKm,
            int page,
            int size,
            Throwable t) {

        logger.warn("⚠️ Fallback acionado: Overpass API falhou ({})", t.getMessage());

        List<AgenciaResponse> cached = repository.findAll().stream()
                .limit(10)
                .map(mapper::toAgenciaResponse)
                .toList();

        Pageable pageable = PageRequest.of(0, cached.isEmpty() ? 10 : cached.size());
        return mapper.toPageResponseFromList(cached, pageable, cached.size());
    }

}
