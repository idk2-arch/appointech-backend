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

    @Override
    public ConfiguracionFranjas guardar(ConfiguracionFranjas configuracion) {
        ConfiguracionFranjasEntity entity = new ConfiguracionFranjasEntity();
        entity.setId(configuracion.getId());
        entity.setDuracionBloqueMinutos(configuracion.getDuracionBloqueMinutos());
        entity.setRadioMaximoKm(configuracion.getRadioMaximoKm());
        entity.setHoraInicioJornada(configuracion.getHoraInicioJornada());
        entity.setHoraFinJornada(configuracion.getHoraFinJornada());
        entity.setTelefonoWhatsappAdmin(configuracion.getTelefonoWhatsappAdmin());
        return toDomain(configuracionFranjasJpaRepository.save(entity));
    }

    private ConfiguracionFranjas toDomain(ConfiguracionFranjasEntity entity) {
        ConfiguracionFranjas configuracion = new ConfiguracionFranjas(
                entity.getId(), entity.getDuracionBloqueMinutos(), entity.getRadioMaximoKm(),
                entity.getHoraInicioJornada(), entity.getHoraFinJornada());
        configuracion.setTelefonoWhatsappAdmin(entity.getTelefonoWhatsappAdmin());
        return configuracion;
    }
}
