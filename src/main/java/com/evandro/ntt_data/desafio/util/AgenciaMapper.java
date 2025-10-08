package com.evandro.ntt_data.desafio.util;

import com.evandro.ntt_data.desafio.domain.Agencia;
import com.evandro.ntt_data.desafio.dto.AgenciaRequest;
import com.evandro.ntt_data.desafio.dto.AgenciaResponse;
import com.evandro.ntt_data.desafio.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
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
                entity.getDistance(),
                entity.getExternalId()
        );
    }

    public PageResponse<AgenciaResponse> toPageResponseFromList(List<AgenciaResponse> responses, Pageable pageable, long totalElements) {
        if (responses == null || responses.isEmpty()) {
            return new PageResponse<>(
                    List.of(),
                    pageable.getPageSize(),
                    0,
                    0,
                    true
            );
        }

        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());
        boolean isLast = (pageable.getPageNumber() + 1) >= totalPages;

        return new PageResponse<>(
                responses,
                pageable.getPageSize(),
                totalElements,
                totalPages,
                isLast
        );
    }
    public List<Agencia> toEntityList(List<AgenciaResponse> responses) {
        if (responses == null) return List.of();

        return responses.stream()
                .filter(Objects::nonNull)
                .map(this::toEntityFromAgenciaResponse)
                .collect(Collectors.toList());
    }


    private Agencia toEntityFromAgenciaResponse(AgenciaResponse response) {
        if (response == null) return null;

        return new Agencia(
                response.id(),
                response.name(),
                response.positionX(),
                response.positionY(),
                response.address(),
                response.externalId(),
                response.distanceKm()
        );
    }

}