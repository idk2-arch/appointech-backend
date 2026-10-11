package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.ConfiguracionFranjas;

import java.util.Optional;

public interface ConfiguracionFranjasRepositoryPort {
    Optional<ConfiguracionFranjas> obtenerActual();
}
