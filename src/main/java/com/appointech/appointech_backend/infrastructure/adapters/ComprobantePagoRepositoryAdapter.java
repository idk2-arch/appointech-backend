package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.ComprobantePago;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.ports.out.ComprobantePagoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.PagoRepositoryPort;
import com.appointech.appointech_backend.infrastructure.entities.ComprobantePagoEntity;
import com.appointech.appointech_backend.infrastructure.repositories.ComprobantePagoJpaRepository;
import com.appointech.appointech_backend.infrastructure.repositories.PagoJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ComprobantePagoRepositoryAdapter implements ComprobantePagoRepositoryPort {

    private final ComprobantePagoJpaRepository comprobanteJpaRepository;
    private final PagoJpaRepository pagoJpaRepository;
    private final PagoRepositoryPort pagoRepositoryPort;

    public ComprobantePagoRepositoryAdapter(ComprobantePagoJpaRepository comprobanteJpaRepository,
                                            PagoJpaRepository pagoJpaRepository,
                                            PagoRepositoryPort pagoRepositoryPort) {
        this.comprobanteJpaRepository = comprobanteJpaRepository;
        this.pagoJpaRepository = pagoJpaRepository;
        this.pagoRepositoryPort = pagoRepositoryPort;
    }

    @Override
    public ComprobantePago guardar(ComprobantePago comprobante) {
        ComprobantePagoEntity entity = new ComprobantePagoEntity();
        entity.setPago(pagoJpaRepository.getReferenceById(comprobante.getPago().getId()));
        entity.setEmitidoEn(comprobante.getEmitidoEn());

        return toDomain(comprobanteJpaRepository.save(entity), comprobante.getPago());
    }

    @Override
    public Optional<ComprobantePago> buscarPorCitaId(Long citaId) {
        return comprobanteJpaRepository.findByPagoCitaId(citaId).map(entity -> {
            Pago pago = pagoRepositoryPort.buscarPorId(entity.getPago().getId()).orElseThrow();
            return toDomain(entity, pago);
        });
    }

    private ComprobantePago toDomain(ComprobantePagoEntity entity, Pago pago) {
        ComprobantePago comprobante = new ComprobantePago();
        comprobante.setId(entity.getId());
        comprobante.setPago(pago);
        comprobante.setEmitidoEn(entity.getEmitidoEn());
        return comprobante;
    }
}
