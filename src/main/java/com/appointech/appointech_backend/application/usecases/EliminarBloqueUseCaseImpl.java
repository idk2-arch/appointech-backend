package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.ResolverTecnicoAutenticado;
import com.appointech.appointech_backend.domain.exception.BloqueNoEncontradoException;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.ports.in.EliminarBloqueUseCase;
import com.appointech.appointech_backend.domain.ports.out.GestionBloquesPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el técnico elimina un bloque propio. Si no existe o es de otro técnico, es "no encontrado".
 * <p>Historia de usuario: <a href="https://rossas2111.atlassian.net/browse/SCRUM-17">SCRUM-17</a> - RF13 - Bloques de indisponibilidad
 */
@Service
public class EliminarBloqueUseCaseImpl implements EliminarBloqueUseCase {

    private final ResolverTecnicoAutenticado resolverTecnico;
    private final GestionBloquesPort gestionBloquesPort;
    private final RegistroLogPort registroLogPort;

    public EliminarBloqueUseCaseImpl(ResolverTecnicoAutenticado resolverTecnico,
                                     GestionBloquesPort gestionBloquesPort,
                                     RegistroLogPort registroLogPort) {
        this.resolverTecnico = resolverTecnico;
        this.gestionBloquesPort = gestionBloquesPort;
        this.registroLogPort = registroLogPort;
    }

    @Override
    @Transactional
    public void eliminar(String correoTecnico, Long bloqueId) {
        Tecnico tecnico = resolverTecnico.resolver(correoTecnico);
        if (!gestionBloquesPort.eliminarDelTecnico(bloqueId, tecnico.getId())) {
            throw new BloqueNoEncontradoException(bloqueId);
        }
        registroLogPort.registrar(NivelLog.INFO, TipoLog.NEGOCIO, "EliminarBloqueUseCaseImpl", correoTecnico,
                "Bloque de indisponibilidad eliminado con id " + bloqueId);
    }
}
