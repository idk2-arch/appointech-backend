package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.Coordenadas;
import com.appointech.appointech_backend.domain.models.Distancia;
import com.appointech.appointech_backend.domain.ports.out.CalculadoraDistanciaPort;

import java.util.List;

/**
 * Distancia en línea recta. Los minutos son una estimación a velocidad media constante.
 */
public class HaversineDistanciaAdapter implements CalculadoraDistanciaPort {

    private static final double RADIO_TIERRA_KM = 6371.0;
    static final double VELOCIDAD_POR_DEFECTO_KM_H = 40.0;

    private final double velocidadMediaKmH;

    public HaversineDistanciaAdapter() {
        this(VELOCIDAD_POR_DEFECTO_KM_H);
    }

    public HaversineDistanciaAdapter(double velocidadMediaKmH) {
        if (velocidadMediaKmH <= 0) {
            throw new IllegalArgumentException("La velocidad media debe ser mayor que cero");
        }
        this.velocidadMediaKmH = velocidadMediaKmH;
    }

    @Override
    public List<Distancia> calcular(Coordenadas origen, List<Coordenadas> destinos) {
        return destinos.stream().map(destino -> {
            double km = calcularKm(origen, destino);
            return new Distancia(km, km / velocidadMediaKmH * 60.0);
        }).toList();
    }

    private double calcularKm(Coordenadas origen, Coordenadas destino) {
        double diferenciaLatitud = Math.toRadians(destino.latitud() - origen.latitud());
        double diferenciaLongitud = Math.toRadians(destino.longitud() - origen.longitud());
        double a = Math.sin(diferenciaLatitud / 2) * Math.sin(diferenciaLatitud / 2)
                + Math.cos(Math.toRadians(origen.latitud())) * Math.cos(Math.toRadians(destino.latitud()))
                * Math.sin(diferenciaLongitud / 2) * Math.sin(diferenciaLongitud / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RADIO_TIERRA_KM * c;
    }
}
