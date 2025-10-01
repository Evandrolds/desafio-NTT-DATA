package com.evandro.ntt_data.desafio.util;

public final class CalcularDistancia {
  private static final double raioDaTerraEmKm = 6371.0088;
  private CalcularDistancia(){}
     /**
     * Calcular a distância em quilômetros entre dois pontos (lat,lon) usando Haversine.
     */
    public static double distanceKm(double positionX, double positionY,double latitude, double longitude) {
        double latRad1 = Math.toRadians(positionX);
        double latRad2 = Math.toRadians(positionY);
        double deltaLat = Math.toRadians(latitude - positionX);
        double deltaLon = Math.toRadians(longitude - positionY);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(latRad1) * Math.cos(latRad2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return raioDaTerraEmKm * c;
    }
}
