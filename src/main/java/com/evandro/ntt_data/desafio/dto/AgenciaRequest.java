package com.evandro.ntt_data.desafio.dto;

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
