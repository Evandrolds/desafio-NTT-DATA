package com.evandro.ntt_data.desafio.repository;

import com.evandro.ntt_data.desafio.domain.Agencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LocalizaAgenciaRepository extends JpaRepository<Agencia, Long> {
    Page<Agencia> findByLatitudeBetweenAndLongitudeBetween(double minLat, double maxLat,
                                                     double minLon, double maxLon,
                                                     Pageable pageable);
    @Query("SELECT a.externalId FROM Agencia a")
    List<String> findAllExternalIds();

    boolean existsByExternalId(String externalId);
}
