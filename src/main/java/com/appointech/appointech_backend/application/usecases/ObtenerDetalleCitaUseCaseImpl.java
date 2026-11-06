package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.DetalleCita;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.ObtenerDetalleCitaUseCase;
import com.appointech.appointech_backend.domain.ports.out.EspecialidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObtenerDetalleCitaUseCaseImpl implements ObtenerDetalleCitaUseCase {

    private final TecnicoRepositoryPort tecnicoRepositoryPort;
    private final EspecialidadRepositoryPort especialidadRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;

    public ObtenerDetalleCitaUseCaseImpl(TecnicoRepositoryPort tecnicoRepositoryPort,
                                         EspecialidadRepositoryPort especialidadRepositoryPort,
                                         UsuarioRepositoryPort usuarioRepositoryPort) {
        this.tecnicoRepositoryPort = tecnicoRepositoryPort;
        this.especialidadRepositoryPort = especialidadRepositoryPort;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public DetalleCita detallar(Cita cita) {
        Usuario tecnico = cita.getTecnicoId() == null ? null
                : tecnicoRepositoryPort.buscarPorId(cita.getTecnicoId()).map(Tecnico::getUsuario).orElse(null);
        String especialidad = cita.getEspecialidadId() == null ? null
                : especialidadRepositoryPort.buscarPorId(cita.getEspecialidadId()).map(Especialidad::getNombre).orElse(null);

        return new DetalleCita(
                cita,
                tecnico != null ? tecnico.getNombre() : null,
                tecnico != null ? tecnico.getApellido() : null,
                tecnico != null ? tecnico.getTelefono() : null,
                especialidad,
                tecnico != null ? tecnico.getFotoClave() : null,
                nombreDeQuienAsigno(cita)
        );
    }

    /** Null cuando la cita no se asignó manualmente o es una fila antigua sin dato de quién la asignó. */
    private String nombreDeQuienAsigno(Cita cita) {
        if (cita.getAsignadaPorId() == null) {
            return null;
        }
        return usuarioRepositoryPort.buscarPorId(cita.getAsignadaPorId())
                .map(admin -> admin.getNombre() + " " + admin.getApellido())
                .orElse(null);
    }
}
