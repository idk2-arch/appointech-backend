package com.appointech.appointech_backend.domain.ports.out;

public interface CalculadorDistanciaPort {
    double calcularKm(double latitudOrigen, double longitudOrigen, double latitudDestino, double longitudDestino);
}
