package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.ConfiguracionFranjas;
import com.appointech.appointech_backend.domain.ports.out.ConfiguracionFranjasRepositoryPort;
import com.appointech.appointech_backend.infrastructure.entities.ConfiguracionFranjasEntity;
import com.appointech.appointech_backend.infrastructure.repositories.ConfiguracionFranjasJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ConfiguracionFranjasRepositoryAdapter implements ConfiguracionFranjasRepositoryPort {

    private final ConfiguracionFranjasJpaRepository configuracionFranjasJpaRepository;

    public ConfiguracionFranjasRepositoryAdapter(ConfiguracionFranjasJpaRepository configuracionFranjasJpaRepository) {
        this.configuracionFranjasJpaRepository = configuracionFranjasJpaRepository;
    }

    @Override
    public Optional<ConfiguracionFranjas> obtenerActual() {
        return configuracionFranjasJpaRepository.findFirstByOrderByIdDesc().map(this::toDomain);
    }

    private ConfiguracionFranjas toDomain(ConfiguracionFranjasEntity entity) {
        return new ConfiguracionFranjas(
                entity.getId(), entity.getDuracionBloqueMinutos(), entity.getRadioMaximoKm(),
                entity.getHoraInicioJornada(), entity.getHoraFinJornada());
    }
}
