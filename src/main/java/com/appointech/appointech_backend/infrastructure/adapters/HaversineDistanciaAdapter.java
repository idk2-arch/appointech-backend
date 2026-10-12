package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.ports.out.CalculadorDistanciaPort;
import org.springframework.stereotype.Component;

@Component
public class HaversineDistanciaAdapter implements CalculadorDistanciaPort {

    private static final double RADIO_TIERRA_KM = 6371.0;

    @Override
    public double calcularKm(double latitudOrigen, double longitudOrigen, double latitudDestino, double longitudDestino) {
        double diferenciaLatitud = Math.toRadians(latitudDestino - latitudOrigen);
        double diferenciaLongitud = Math.toRadians(longitudDestino - longitudOrigen);
        double a = Math.sin(diferenciaLatitud / 2) * Math.sin(diferenciaLatitud / 2)
                + Math.cos(Math.toRadians(latitudOrigen)) * Math.cos(Math.toRadians(latitudDestino))
                * Math.sin(diferenciaLongitud / 2) * Math.sin(diferenciaLongitud / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RADIO_TIERRA_KM * c;
    }
}
