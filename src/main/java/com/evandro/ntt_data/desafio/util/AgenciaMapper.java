package com.evandro.ntt_data.desafio.util;

import com.evandro.ntt_data.desafio.domain.Agencia;
import com.evandro.ntt_data.desafio.dto.AgenciaRequest;
import com.evandro.ntt_data.desafio.dto.AgenciaResponse;
import com.evandro.ntt_data.desafio.dto.DistanciaResponse;
import com.evandro.ntt_data.desafio.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AgenciaMapper {

    public Agencia toEntity(AgenciaRequest dto) {
        if (dto == null) return null;
        Agencia agencia = new Agencia();
        agencia.setName(dto.name());
        agencia.setLatitude(dto.latitude());
        agencia.setLongitude(dto.longitude());
        agencia.setAddress(dto.address());
        return agencia;
    }

    public AgenciaResponse toAgenciaResponse(Agencia entity) {
        if (entity == null) return null;
        return new AgenciaResponse(
                entity.getId(),
                entity.getName(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getAddress(),
                entity.getDistance()
        );
    }

    public DistanciaResponse toDistanciaResponse(Agencia entity) {
        if (entity == null) return null;
        return new DistanciaResponse(
                entity.getId(),
                entity.getName(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getAddress(),
                entity.getDistance(),
                entity.getExternalId()
        );
    }

    public List<DistanciaResponse> toDistanciaResponseList(List<Agencia> agencias) {
        if (agencias == null) return List.of();
        return agencias.stream()
                .map(this::toDistanciaResponse)
                .collect(Collectors.toList());
    }
    public List<Agencia> toListDistanciaResponse(PageResponse<DistanciaResponse> agencias) {
        if (agencias == null) return List.of();
        return agencias.content().stream().map(a -> new Agencia(
                a.id(),
                a.name(),
                a.positionX(),
                a.positionY(),
                a.address(),
                a.externalId(),
                a.distanceKm()

        )
        ).collect(Collectors.toList());
    }

    public PageResponse<DistanciaResponse> toPageResponseDistanceResponse(Page<Agencia> agencias) {
        List<DistanciaResponse> content = agencias.getContent().stream()
                .map(this::toDistanciaResponse)
                .collect(Collectors.toList());

        Page<DistanciaResponse> pageImpl = new PageImpl<>(
                content,
                agencias.getPageable(),
                agencias.getTotalElements()
        );

        return PageResponse.of(pageImpl);
    }

    public DistanciaResponse toDistanciaResponse(Agencia agencia,Double distancia){
       return new DistanciaResponse(
                agencia.getId(),
                agencia.getName(),
                agencia.getLatitude(),
                agencia.getLongitude(),
                agencia.getAddress(),
                distancia,
               agencia.getExternalId()
        );

    }

    // Converte Entity → DistanciaResponse (com distância calculada)
    public DistanciaResponse toDistanciaResponse(Agencia agencia, double distanciaKm) {
        return new DistanciaResponse(
                agencia.getId(),
                agencia.getName(),
                agencia.getLatitude(),
                agencia.getLongitude(),
                agencia.getAddress(),
                distanciaKm,
                agencia.getExternalId()
        );
    }

    // Converte lista de entidades → lista de respostas
    public List<DistanciaResponse> toDistanciaResponse(List<Agencia> agencias) {
        return agencias.stream()
                .map(this::toDistanciaResponse)
                .collect(Collectors.toList());
    }


    public List<Agencia> toEntity(List<DistanciaResponse> responses) {
        return responses.stream()
                .map(r -> new Agencia(
                        r.id(),
                        r.name(),
                        r.positionX(),
                        r.positionY(),
                        r.address(),
                        r.externalId(),
                        r.distanceKm()
                ))
                .collect(Collectors.toList());
    }
    public PageResponse<DistanciaResponse> toPageResponse(List<Agencia> agencias) {
        if (agencias == null || agencias.isEmpty()) {
            return new PageResponse<>(
                    List.of(),
                    0,
                    0,
                    0,
                    0,
                    true
            );
        }

        List<DistanciaResponse> content = agencias.stream()
                .map(a -> new DistanciaResponse(
                        a.getId(),
                        a.getName(),
                        a.getLatitude(),
                        a.getLongitude(),
                        a.getAddress(),
                        a.getDistance(),
                        a.getExternalId()
                ))
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                0,
                content.size(),
                content.size(),
                1,
                true
        );
    }

}
