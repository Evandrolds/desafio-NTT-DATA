package com.evandro.ntt_data.desafio;

import com.evandro.ntt_data.desafio.util.CalcularDistancia;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CalcularDIstanciaTest {

    @Test
    void distanceBetweenSamePointsIsZero() {
        double d = CalcularDistancia.distanceKm(0, 0, 0, 0);
        assertEquals(0.0, d, 1e-9);
    }

    @Test
    void distanceApproximation() {
        // São Paulo (-23.55, -46.63) até Rio de Janeiro (-22.90, -43.20) ~ 357 km (approx)
        double d = CalcularDistancia.distanceKm(-23.55, -46.63, -22.90, -43.20);
        assertTrue(d > 300 && d < 450);
    }
}
