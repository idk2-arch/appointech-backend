package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.GestorCambiosCita;
import com.appointech.appointech_backend.application.services.ResolverClienteAutenticado;
import com.appointech.appointech_backend.domain.exception.SolicitudNoEncontradaException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.ports.in.CancelarCitaClienteUseCase;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el cliente cancela su cita. La franja queda libre porque la disponibilidad se calcula
 * sobre las citas no canceladas del técnico.
 * <p>Historias de usuario: SCRUM-21 (cancelar), SCRUM-20 (notificar).
 */
@Service
public class CancelarCitaClienteUseCaseImpl implements CancelarCitaClienteUseCase {

    private final ResolverClienteAutenticado resolverCliente;
    private final CitaRepositoryPort citaRepositoryPort;
    private final GestorCambiosCita gestor;

    public CancelarCitaClienteUseCaseImpl(ResolverClienteAutenticado resolverCliente,
                                          CitaRepositoryPort citaRepositoryPort,
                                          GestorCambiosCita gestor) {
        this.resolverCliente = resolverCliente;
        this.citaRepositoryPort = citaRepositoryPort;
        this.gestor = gestor;
    }

    @Override
    @Transactional
    public Cita cancelar(String correoCliente, Long citaId, String motivo) {
        Cliente cliente = resolverCliente.resolver(correoCliente);
        Cita cita = citaRepositoryPort.buscarPorId(citaId)
                .filter(c -> cliente.getId().equals(c.getClienteId()))
                .orElseThrow(() -> new SolicitudNoEncontradaException(citaId));

        EstadoCita anterior = cita.getEstado();
        cita.cancelarPorCliente();
        String motivoLimpio = motivo == null || motivo.isBlank() ? null : motivo.strip();
        Cita guardada = gestor.guardarConHistorial(cita, anterior, correoCliente, motivoLimpio);

        String detalle = motivoLimpio == null ? "" : " Motivo: " + motivoLimpio;
        gestor.notificarAlCliente(guardada, "Tu cita #" + guardada.getId() + " fue cancelada." + detalle);
        gestor.notificarAlTecnico(guardada, "El cliente canceló la cita #" + guardada.getId() + "." + detalle);
        return guardada;
    }
}
