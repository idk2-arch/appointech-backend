package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.VerificadorAccesoCita;
import com.appointech.appointech_backend.domain.exception.ComprobanteNoDisponibleException;
import com.appointech.appointech_backend.domain.exception.PagoNoEncontradoException;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.ComprobanteDetalle;
import com.appointech.appointech_backend.domain.models.ComprobantePago;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.ConsultarComprobanteUseCase;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ComprobantePagoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.PagoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import org.springframework.stereotype.Service;

/**
 * Caso de uso: consultar el comprobante de una cita (cliente dueño, técnico asignado o administrador). El comprobante
 * se genera solo al quedar PAGADO el pago; aquí únicamente se consulta.
 * <p>Historias de usuario: SCRUM-26 (RF24 - comprobante).
 */
@Service
public class ConsultarComprobanteUseCaseImpl implements ConsultarComprobanteUseCase {

    private final VerificadorAccesoCita verificadorAcceso;
    private final PagoRepositoryPort pagoRepositoryPort;
    private final ComprobantePagoRepositoryPort comprobanteRepositoryPort;
    private final ClienteRepositoryPort clienteRepositoryPort;
    private final TecnicoRepositoryPort tecnicoRepositoryPort;
    private final EspecialidadRepositoryPort especialidadRepositoryPort;

    public ConsultarComprobanteUseCaseImpl(VerificadorAccesoCita verificadorAcceso,
                                           PagoRepositoryPort pagoRepositoryPort,
                                           ComprobantePagoRepositoryPort comprobanteRepositoryPort,
                                           ClienteRepositoryPort clienteRepositoryPort,
                                           TecnicoRepositoryPort tecnicoRepositoryPort,
                                           EspecialidadRepositoryPort especialidadRepositoryPort) {
        this.verificadorAcceso = verificadorAcceso;
        this.pagoRepositoryPort = pagoRepositoryPort;
        this.comprobanteRepositoryPort = comprobanteRepositoryPort;
        this.clienteRepositoryPort = clienteRepositoryPort;
        this.tecnicoRepositoryPort = tecnicoRepositoryPort;
        this.especialidadRepositoryPort = especialidadRepositoryPort;
    }

    @Override
    public ComprobanteDetalle consultar(String correo, Long citaId) {
        Cita cita = verificadorAcceso.exigirAcceso(correo, citaId);
        Pago pago = pagoRepositoryPort.buscarPorCitaId(citaId).orElseThrow(() -> new PagoNoEncontradoException(citaId));
        if (pago.getEstado() != EstadoPago.PAGADO) {
            throw new ComprobanteNoDisponibleException(
                    "El comprobante solo está disponible cuando el pago está PAGADO; el pago está " + pago.getEstado());
        }
        ComprobantePago comprobante = comprobanteRepositoryPort.buscarPorPagoId(pago.getId())
                .orElseThrow(() -> new ComprobanteNoDisponibleException(
                        "El comprobante de esta cita aún no está disponible"));

        return new ComprobanteDetalle(comprobante.numero(), comprobante.emitidoEn(), citaId,
                nombreCliente(cita), nombreTecnico(cita), nombreEspecialidad(cita), pago.getMontoFinal(),
                pago.getMetodo(), pago.getReferenciaExterna(), pago.getPagadoEn());
    }

    private String nombreCliente(Cita cita) {
        return clienteRepositoryPort.buscarPorId(cita.getClienteId())
                .map(cliente -> nombreCompleto(cliente.getUsuario())).orElse(null);
    }

    private String nombreTecnico(Cita cita) {
        if (cita.getTecnicoId() == null) {
            return null;
        }
        return tecnicoRepositoryPort.buscarPorId(cita.getTecnicoId())
                .map(tecnico -> nombreCompleto(tecnico.getUsuario())).orElse(null);
    }

    private String nombreEspecialidad(Cita cita) {
        return especialidadRepositoryPort.buscarPorId(cita.getEspecialidadId())
                .map(Especialidad::getNombre).orElse(null);
    }

    private String nombreCompleto(Usuario usuario) {
        return (usuario.getNombre() + " " + usuario.getApellido()).strip();
    }
}
