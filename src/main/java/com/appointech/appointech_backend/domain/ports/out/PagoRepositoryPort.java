package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.Pago;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PagoRepositoryPort {
    Pago guardar(Pago pago);
    Optional<Pago> buscarPorCitaId(Long citaId);
    /** Pagos de esas citas (las que aún no tienen pago no aparecen). */
    List<Pago> buscarPorCitaIds(Collection<Long> citaIds);
    /** Toma el bloqueo de la fila para serializar cambios concurrentes (sincronizaciones, confirmaciones). */
    Optional<Pago> buscarPorCitaIdBloqueando(Long citaId);
}
