package com.evandro.ntt_data.desafio.controller;

import com.evandro.ntt_data.desafio.dto.*;
import com.evandro.ntt_data.desafio.repository.LocalizaAgenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agencia")
public class LocalizaAgenciaController {

    private final LocalizaAgenciaService service;

    @Autowired
    public LocalizaAgenciaController(LocalizaAgenciaService service){
        this.service = service;
    }
    @Operation(summary = "Cadastrar agencia")
    @PostMapping("/cadastrar")
    public ResponseEntity<AgenciaResponse> saveAgencia(@RequestBody(required = true) AgenciaRequest requestDTO){
      return new ResponseEntity<>(service.createAgencia(requestDTO),HttpStatus.CREATED);
    }
    @Operation(summary = "Buscar agencias próximas ordenadas por proximidade")
    @GetMapping("/agencias/closest")
    public ResponseEntity<PageResponse<DistanciaResponse>> findClosest(
            double latitude,
            double longitude,
            @RequestParam(required = false, defaultValue = "50") Double maxDistanceKm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<DistanciaResponse> result = service.findClosestDistance(latitude, longitude, maxDistanceKm,page,size);
        return ResponseEntity.ok(result);
    }

}
