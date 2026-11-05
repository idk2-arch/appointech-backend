package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.MetodoPago;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.models.exception.CitaNoEncontradaException;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.PagoRepositoryPort;
import com.appointech.appointech_backend.infrastructure.entities.EstadoPagoEntity;
import com.appointech.appointech_backend.infrastructure.entities.MetodoPagoEntity;
import com.appointech.appointech_backend.infrastructure.entities.PagoEntity;
import com.appointech.appointech_backend.infrastructure.repositories.CitaJpaRepository;
import com.appointech.appointech_backend.infrastructure.repositories.PagoJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PagoRepositoryAdapter implements PagoRepositoryPort {

    private final PagoJpaRepository pagoJpaRepository;
    private final CitaJpaRepository citaJpaRepository;
    private final CitaRepositoryPort citaRepositoryPort;

    public PagoRepositoryAdapter(PagoJpaRepository pagoJpaRepository,
                                 CitaJpaRepository citaJpaRepository,
                                 CitaRepositoryPort citaRepositoryPort) {
        this.pagoJpaRepository = pagoJpaRepository;
        this.citaJpaRepository = citaJpaRepository;
        this.citaRepositoryPort = citaRepositoryPort;
    }

    @Override
    public Pago guardar(Pago pago) {
        PagoEntity entity = pago.getId() == null
                ? new PagoEntity()
                : pagoJpaRepository.findById(pago.getId()).orElseGet(PagoEntity::new);

        entity.setCita(citaJpaRepository.getReferenceById(pago.getCita().getId()));
        entity.setMontoCotizado(pago.getMontoCotizado());
        entity.setMontoFinal(pago.getMontoFinal());
        entity.setMetodo(pago.getMetodo() == null ? null : MetodoPagoEntity.valueOf(pago.getMetodo().name()));
        entity.setEstado(EstadoPagoEntity.valueOf(pago.getEstado().name()));
        entity.setReferenciaExterna(pago.getReferenciaExterna());
        entity.setPagadoEn(pago.getPagadoEn());

        return toDomain(pagoJpaRepository.save(entity), pago.getCita());
    }

    @Override
    public Optional<Pago> buscarPorId(Long id) {
        return pagoJpaRepository.findById(id).map(entity -> toDomain(entity, cargarCita(entity)));
    }

    @Override
    public Optional<Pago> buscarPorCitaId(Long citaId) {
        return pagoJpaRepository.findByCitaId(citaId).map(entity -> toDomain(entity, cargarCita(entity)));
    }

    private Cita cargarCita(PagoEntity entity) {
        Long citaId = entity.getCita().getId();
        return citaRepositoryPort.buscarPorId(citaId).orElseThrow(() -> new CitaNoEncontradaException(citaId));
    }

    private Pago toDomain(PagoEntity entity, Cita cita) {
        Pago pago = new Pago();
        pago.setId(entity.getId());
        pago.setCita(cita);
        pago.setMontoCotizado(entity.getMontoCotizado());
        pago.setMontoFinal(entity.getMontoFinal());
        pago.setMetodo(entity.getMetodo() == null ? null : MetodoPago.valueOf(entity.getMetodo().name()));
        pago.setEstado(EstadoPago.valueOf(entity.getEstado().name()));
        pago.setReferenciaExterna(entity.getReferenciaExterna());
        pago.setPagadoEn(entity.getPagadoEn());
        return pago;
    }
}
