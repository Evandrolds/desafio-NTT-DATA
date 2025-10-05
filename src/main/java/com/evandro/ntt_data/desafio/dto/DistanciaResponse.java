package com.evandro.ntt_data.desafio.dto;

public record DistanciaResponse(
        Long id,
        String name,
        Double positionX,
        Double positionY,
        String address,
        Double distanceKm,
        String externalId
){}
