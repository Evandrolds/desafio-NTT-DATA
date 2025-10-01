package com.evandro.ntt_data.desafio.dto;

import jakarta.validation.constraints.NotNull;

public record CoordenadasRequest(
        @NotNull
        Double latitude,
        @NotNull
        Double longitude,
        Double maxDistanceKm,
        Integer limit
){}
