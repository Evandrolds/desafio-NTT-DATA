package com.evandro.ntt_data.desafio.util;

import com.evandro.ntt_data.desafio.domain.Agencia;
import com.evandro.ntt_data.desafio.dto.AgenciaRequest;
import com.evandro.ntt_data.desafio.dto.AgenciaResponse;
import com.evandro.ntt_data.desafio.dto.DistanciaResponse;
import org.springframework.stereotype.Component;

@Component
public class AgenciaMapper {

    public Agencia toEntity(AgenciaRequest requestDTO){
        return new Agencia(
                null,
                requestDTO.name(),
                requestDTO.latitude(),
                requestDTO.longitude(),
                requestDTO.address()
        );
    }
    public DistanciaResponse toDistanciaResponse(Agencia requestDTO, double distancia){
       return new DistanciaResponse(
                requestDTO.getId(),
                requestDTO.getName(),
                requestDTO.getLatitude(),
                requestDTO.getLongitude(),
                requestDTO.getAddress(),
                distancia
        );

    }
    public AgenciaResponse toAgenciaResponse(Agencia agencia){
        return new AgenciaResponse(
                agencia.getId(),
                agencia.getName(),
                agencia.getLatitude(),
                agencia.getLongitude(),
                agencia.getAddress()
        );
    }
}
