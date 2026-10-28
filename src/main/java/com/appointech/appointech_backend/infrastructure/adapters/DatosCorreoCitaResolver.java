package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.DetalleCita;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.ObtenerDetalleCitaUseCase;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import org.springframework.stereotype.Component;

/**
 * Reúne los textos legibles (cliente, servicio, técnico) que el correo necesita y que la cita solo guarda como ids.
 * Reutiliza el caso de uso que ya arma el detalle de la cita, sin duplicar sus consultas.
 */
@Component
public class DatosCorreoCitaResolver {

    /** Cadenas vacías cuando no hay dato (p. ej. cita sin técnico). */
    public record DatosCorreoCita(String nombreCliente, String servicio, String tecnico) {
    }

    private final ClienteRepositoryPort clienteRepositoryPort;
    private final ObtenerDetalleCitaUseCase obtenerDetalleCitaUseCase;

    public DatosCorreoCitaResolver(ClienteRepositoryPort clienteRepositoryPort,
                                   ObtenerDetalleCitaUseCase obtenerDetalleCitaUseCase) {
        this.clienteRepositoryPort = clienteRepositoryPort;
        this.obtenerDetalleCitaUseCase = obtenerDetalleCitaUseCase;
    }

    public DatosCorreoCita resolver(Cita cita) {
        String nombreCliente = clienteRepositoryPort.buscarPorId(cita.getClienteId())
                .map(Cliente::getUsuario)
                .map(this::nombreCompleto)
                .orElse("");
        DetalleCita detalle = obtenerDetalleCitaUseCase.detallar(cita);
        return new DatosCorreoCita(
                nombreCliente,
                detalle.especialidadNombre() == null ? "" : detalle.especialidadNombre(),
                unir(detalle.tecnicoNombre(), detalle.tecnicoApellido()));
    }

    private String nombreCompleto(Usuario usuario) {
        return unir(usuario.getNombre(), usuario.getApellido());
    }

    private String unir(String nombre, String apellido) {
        return ((nombre == null ? "" : nombre) + " " + (apellido == null ? "" : apellido)).strip();
    }
}
