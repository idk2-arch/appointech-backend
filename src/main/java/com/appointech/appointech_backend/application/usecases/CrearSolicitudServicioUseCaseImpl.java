package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.Coordenadas;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.exception.ClienteNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.EspecialidadNoEncontradaException;
import com.appointech.appointech_backend.domain.ports.in.CrearSolicitudServicioUseCase;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Caso de uso: crear una solicitud de servicio en estado PENDIENTE.
 * <p>Historias de usuario:
 * <ul>
 * <li><a href="https://rossas2111.atlassian.net/browse/SCRUM-10">SCRUM-10</a> - RF02 - Crear solicitud de servicio</li>
 * </ul>
 */
@Service
public class CrearSolicitudServicioUseCaseImpl implements CrearSolicitudServicioUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final ClienteRepositoryPort clienteRepositoryPort;
    private final EspecialidadRepositoryPort especialidadRepositoryPort;
    private final CitaRepositoryPort citaRepositoryPort;
    private final RegistroLogPort registroLogPort;

    public CrearSolicitudServicioUseCaseImpl(UsuarioRepositoryPort usuarioRepositoryPort,
                                             ClienteRepositoryPort clienteRepositoryPort,
                                             EspecialidadRepositoryPort especialidadRepositoryPort,
                                             CitaRepositoryPort citaRepositoryPort,
                                             RegistroLogPort registroLogPort) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.clienteRepositoryPort = clienteRepositoryPort;
        this.especialidadRepositoryPort = especialidadRepositoryPort;
        this.citaRepositoryPort = citaRepositoryPort;
        this.registroLogPort = registroLogPort;
    }

    @Override
    public Cita crear(String correoCliente, Long especialidadId, String descripcionProblema,
                      String direccionServicio, Double latitud, Double longitud) {
        Coordenadas ubicacion = Coordenadas.de(latitud, longitud);

        Usuario usuario = usuarioRepositoryPort.buscarPorCorreo(correoCliente)
                .orElseThrow(() -> new ClienteNoEncontradoException(correoCliente));
        Cliente cliente = clienteRepositoryPort.buscarPorUsuarioId(usuario.getId())
                .orElseThrow(() -> new ClienteNoEncontradoException(correoCliente));

        especialidadRepositoryPort.buscarPorId(especialidadId)
                .filter(especialidad -> especialidad.isActivo())
                .orElseThrow(() -> new EspecialidadNoEncontradaException(List.of(especialidadId)));

        LocalDateTime ahora = LocalDateTime.now();

        Cita solicitud = new Cita();
        solicitud.setClienteId(cliente.getId());
        solicitud.setEspecialidadId(especialidadId);
        solicitud.setDescripcionProblema(descripcionProblema);
        solicitud.setDireccionServicio(direccionServicio);
        solicitud.setLatitud(ubicacion.latitud());
        solicitud.setLongitud(ubicacion.longitud());
        solicitud.setEstado(EstadoCita.PENDIENTE);
        solicitud.setCreadaEn(ahora);
        solicitud.setActualizadaEn(ahora);

        Cita solicitudGuardada = citaRepositoryPort.guardar(solicitud);
        registroLogPort.registrar(NivelLog.INFO, TipoLog.NEGOCIO, "CrearSolicitudServicioUseCaseImpl", correoCliente,
                "Solicitud de servicio creada con id " + solicitudGuardada.getId());
        return solicitudGuardada;
    }
}
