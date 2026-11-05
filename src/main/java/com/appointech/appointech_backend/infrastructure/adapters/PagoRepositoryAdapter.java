package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.ports.out.PagoRepositoryPort;
import com.appointech.appointech_backend.infrastructure.entities.PagoEntity;
import com.appointech.appointech_backend.infrastructure.repositories.PagoJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class PagoRepositoryAdapter implements PagoRepositoryPort {

    private final PagoJpaRepository pagoJpaRepository;

    public PagoRepositoryAdapter(PagoJpaRepository pagoJpaRepository) {
        this.pagoJpaRepository = pagoJpaRepository;
    }

    @Override
    public Pago guardar(Pago pago) {
        return aDominio(pagoJpaRepository.save(aEntidad(pago)));
    }

    @Override
    public Optional<Pago> buscarPorCitaId(Long citaId) {
        return pagoJpaRepository.findByCitaId(citaId).map(this::aDominio);
    }

    @Override
    public List<Pago> buscarPorCitaIds(Collection<Long> citaIds) {
        return pagoJpaRepository.findByCitaIdIn(citaIds).stream().map(this::aDominio).toList();
    }

    @Override
    public Optional<Pago> buscarPorCitaIdBloqueando(Long citaId) {
        return pagoJpaRepository.buscarPorCitaIdBloqueando(citaId).map(this::aDominio);
    }

    private PagoEntity aEntidad(Pago pago) {
        return new PagoEntity(pago.getId(), pago.getCitaId(), pago.getMontoCotizado(), pago.getMontoFinal(),
                pago.getMetodo(), pago.getEstado(), pago.getReferenciaExterna(), pago.getPagadoEn(),
                pago.getIdTransaccionExterna(), pago.getIntentosPago());
    }

    private Pago aDominio(PagoEntity entity) {
        Pago pago = new Pago();
        pago.setId(entity.getId());
        pago.setCitaId(entity.getCitaId());
        pago.setMontoCotizado(entity.getMontoCotizado());
        pago.setMontoFinal(entity.getMontoFinal());
        pago.setMetodo(entity.getMetodo());
        pago.setEstado(entity.getEstado());
        pago.setReferenciaExterna(entity.getReferenciaExterna());
        pago.setPagadoEn(entity.getPagadoEn());
        pago.setIdTransaccionExterna(entity.getIdTransaccionExterna());
        pago.setIntentosPago(entity.getIntentosPago());
        return pago;
    }
}
