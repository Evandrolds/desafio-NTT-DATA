package com.evandro.ntt_data.desafio.service;

import com.evandro.ntt_data.desafio.domain.Agencia;
import com.evandro.ntt_data.desafio.exception.ApiExceptionHandler;
import com.evandro.ntt_data.desafio.util.CalcularDistancia;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AgenciaExternaService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(AgenciaExternaService.class);

    public AgenciaExternaService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public List<Agencia> buscarAgenciasProximas(double latitude, double longitude, double raioKm) {
        try {
            String url = buildOSMUrl(latitude, longitude, raioKm);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return parseOSMResponse(response.getBody(), latitude, longitude);
            }

        } catch (Exception e) {
           logger.warn("Erro ao buscar agências no OSM: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    private String buildOSMUrl(double lat, double lon, double raioKm) {
        return String.format(
                "https://nominatim.openstreetmap.org/search?format=json&amenity=bank&lat=%f&lon=%f&radius=%f&limit=50",
                lat, lon, raioKm * 1000
        );
    }

    private List<Agencia> parseOSMResponse(String jsonResponse, double latitude, double longitude) {
        try {
            JsonNode nodes = objectMapper.readTree(jsonResponse);
            List<Agencia> agencias = new ArrayList<>();

            for (JsonNode node : nodes) {
                Agencia agencia = new Agencia();
                agencia.setName(node.has("name") ? node.get("name").asText() : "Agência Bancária");
                agencia.setAddress(node.has("display_name") ? node.get("display_name").asText() : "");
                agencia.setLatitude(node.get("lat").asDouble());
                agencia.setLongitude(node.get("lon").asDouble());
                agencia.setExternalId("OSM_" + node.get("osm_id").asText());

                // Calcular distância do ponto central
                double distancia = CalcularDistancia.distanceKm(
                        latitude, longitude,
                        agencia.getLatitude(), agencia.getLongitude()
                );
                agencia.setDistance(distancia);

                agencias.add(agencia);
            }

            return agencias;

        } catch (Exception e) {
            logger.error("Erro ao parsear resposta OSM: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}