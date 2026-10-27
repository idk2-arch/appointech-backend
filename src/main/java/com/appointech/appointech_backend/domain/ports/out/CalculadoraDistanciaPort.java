package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.Coordenadas;
import com.appointech.appointech_backend.domain.models.Distancia;

import java.util.List;

public interface CalculadoraDistanciaPort {

    /**
     * Distancia desde el origen a cada destino, en el mismo orden de la lista recibida.
     */
    List<Distancia> calcular(Coordenadas origen, List<Coordenadas> destinos);
}
