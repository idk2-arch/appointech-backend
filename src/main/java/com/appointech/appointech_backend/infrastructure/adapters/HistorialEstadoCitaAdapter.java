package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.HistorialEstadoCita;
import com.appointech.appointech_backend.domain.ports.out.HistorialEstadoCitaPort;
import com.appointech.appointech_backend.infrastructure.entities.EstadoCitaEntity;
import com.appointech.appointech_backend.infrastructure.entities.HistorialEstadoCitaEntity;
import com.appointech.appointech_backend.infrastructure.repositories.HistorialEstadoCitaJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HistorialEstadoCitaAdapter implements HistorialEstadoCitaPort {

    private final HistorialEstadoCitaJpaRepository historialJpa;

    public HistorialEstadoCitaAdapter(HistorialEstadoCitaJpaRepository historialJpa) {
        this.historialJpa = historialJpa;
    }

    @Override
    public HistorialEstadoCita registrar(HistorialEstadoCita entrada) {
        HistorialEstadoCitaEntity entity = new HistorialEstadoCitaEntity();
        entity.setCitaId(entrada.citaId());
        entity.setEstadoAnterior(entrada.estadoAnterior() == null ? null
                : EstadoCitaEntity.valueOf(entrada.estadoAnterior().name()));
        entity.setEstadoNuevo(EstadoCitaEntity.valueOf(entrada.estadoNuevo().name()));
        entity.setFecha(entrada.fecha());
        entity.setActor(entrada.actor());
        entity.setMotivo(entrada.motivo());
        return aDominio(historialJpa.save(entity));
    }

    @Override
    public List<HistorialEstadoCita> listarPorCita(Long citaId) {
        return historialJpa.findByCitaIdOrderByFechaAscIdAsc(citaId).stream().map(this::aDominio).toList();
    }

    private HistorialEstadoCita aDominio(HistorialEstadoCitaEntity entity) {
        return new HistorialEstadoCita(
                entity.getId(),
                entity.getCitaId(),
                entity.getEstadoAnterior() == null ? null : EstadoCita.valueOf(entity.getEstadoAnterior().name()),
                EstadoCita.valueOf(entity.getEstadoNuevo().name()),
                entity.getFecha(),
                entity.getActor(),
                entity.getMotivo());
    }
}
