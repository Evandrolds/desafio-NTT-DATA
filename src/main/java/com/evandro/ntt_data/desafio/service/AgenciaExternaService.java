package com.evandro.ntt_data.desafio.service;

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
