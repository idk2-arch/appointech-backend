package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.ComprobantePago;
import com.appointech.appointech_backend.domain.ports.out.ComprobantePagoRepositoryPort;
import com.appointech.appointech_backend.infrastructure.entities.ComprobantePagoEntity;
import com.appointech.appointech_backend.infrastructure.repositories.ComprobantePagoJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ComprobantePagoRepositoryAdapter implements ComprobantePagoRepositoryPort {

    private final ComprobantePagoJpaRepository comprobanteJpaRepository;

    public ComprobantePagoRepositoryAdapter(ComprobantePagoJpaRepository comprobanteJpaRepository) {
        this.comprobanteJpaRepository = comprobanteJpaRepository;
    }

    @Override
    public ComprobantePago guardar(ComprobantePago comprobante) {
        ComprobantePagoEntity entity = new ComprobantePagoEntity(comprobante.id(), comprobante.pagoId(),
                comprobante.emitidoEn());
        return aDominio(comprobanteJpaRepository.save(entity));
    }

    @Override
    public Optional<ComprobantePago> buscarPorPagoId(Long pagoId) {
        return comprobanteJpaRepository.findByPagoId(pagoId).map(this::aDominio);
    }

    private ComprobantePago aDominio(ComprobantePagoEntity entity) {
        return new ComprobantePago(entity.getId(), entity.getPagoId(), entity.getEmitidoEn());
    }
}
