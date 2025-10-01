package com.evandro.ntt_data.desafio.controller;

import com.evandro.ntt_data.desafio.dto.*;
import com.evandro.ntt_data.desafio.repository.LocalizaAgenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agencia")
@Tag(name = "Agencia",description = "Cadastre e busque a agencia mais próxima de voce")
public class LocalizaAgenciaController {

    private final LocalizaAgenciaService service;

    @Autowired
    public LocalizaAgenciaController(LocalizaAgenciaService service){
        this.service = service;
    }
    @Operation(summary = "Cadastrar agencia")
    @PostMapping("/cadastrar")
    public ResponseEntity<AgenciaResponse> saveAgencia(@RequestBody AgenciaRequest requestDTO){
      return new ResponseEntity<>(service.createAgencia(requestDTO),HttpStatus.CREATED);
    }
    @Operation(summary = "Buscar agencias próximas ordenadas por proximidade")
    @GetMapping("/distancia")
    public ResponseEntity<PageResponse<DistanciaResponse>> closest(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(required = false) Double maxDistanceKm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<DistanciaResponse> result =
                service.findClosestDistance(latitude, longitude, maxDistanceKm, page, size);
        return ResponseEntity.ok(result);
    }
}
