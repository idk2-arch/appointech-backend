package com.appointech.appointech_backend.application.services;

import com.appointech.appointech_backend.domain.exception.PagoNoEncontradoException;
import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.ports.out.PagoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import org.springframework.stereotype.Component;

/** Acceso común al pago de una cita para los casos de uso: obtenerlo (con bloqueo) y guardarlo dejando auditoría. */
@Component
public class PagosDeCita {

    private final PagoRepositoryPort pagoRepositoryPort;
    private final RegistroLogPort registroLogPort;

    public PagosDeCita(PagoRepositoryPort pagoRepositoryPort, RegistroLogPort registroLogPort) {
        this.pagoRepositoryPort = pagoRepositoryPort;
        this.registroLogPort = registroLogPort;
    }

    /** Pago de la cita con la fila bloqueada; el llamador debe estar dentro de una transacción. */
    public Pago obtenerBloqueando(Long citaId) {
        return pagoRepositoryPort.buscarPorCitaIdBloqueando(citaId)
                .orElseThrow(() -> new PagoNoEncontradoException(citaId));
    }

    /** Igual que obtenerBloqueando, pero crea el pago si la cita todavía no tiene uno. */
    public Pago obtenerOCrearBloqueando(Long citaId) {
        return pagoRepositoryPort.buscarPorCitaIdBloqueando(citaId)
                .orElseGet(() -> pagoRepositoryPort.guardar(Pago.nuevo(citaId)));
    }

    /** Guarda el pago y, si cambió de estado, lo deja en el log de auditoría (sin montos ni datos del cliente). */
    public Pago guardar(Pago pago, EstadoPago estadoAnterior, String actor) {
        Pago guardado = pagoRepositoryPort.guardar(pago);
        if (estadoAnterior != guardado.getEstado()) {
            registroLogPort.registrar(NivelLog.INFO, TipoLog.AUDITORIA, "PagosDeCita", actor,
                    "Pago de la cita " + guardado.getCitaId() + ": " + estadoAnterior + " -> " + guardado.getEstado());
        }
        return guardado;
    }
}
