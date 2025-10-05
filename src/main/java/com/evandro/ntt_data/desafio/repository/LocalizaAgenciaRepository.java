package com.evandro.ntt_data.desafio.repository;

import com.evandro.ntt_data.desafio.domain.Agencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalizaAgenciaRepository extends JpaRepository<Agencia, Long> {
    Page<Agencia> findByLatitudeBetweenAndLongitudeBetween(double minLat, double maxLat,
                                                     double minLon, double maxLon,
                                                     Pageable pageable);
}
