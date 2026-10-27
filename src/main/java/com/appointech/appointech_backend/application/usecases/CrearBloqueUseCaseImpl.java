package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.ResolverTecnicoAutenticado;
import com.appointech.appointech_backend.domain.exception.BloqueInvalidoException;
import com.appointech.appointech_backend.domain.exception.BloqueSolapadoException;
import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.ports.in.CrearBloqueUseCase;
import com.appointech.appointech_backend.domain.ports.out.BloqueIndisponibilidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ConsultaCitasDelTecnicoPort;
import com.appointech.appointech_backend.domain.ports.out.GestionBloquesPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Caso de uso: el técnico bloquea un rango en el que no puede atender.
 * <p>Historia de usuario: <a href="https://rossas2111.atlassian.net/browse/SCRUM-17">SCRUM-17</a> - RF13 - Bloques de indisponibilidad
 */
@Service
public class CrearBloqueUseCaseImpl implements CrearBloqueUseCase {

    static final List<EstadoCita> ESTADOS_QUE_OCUPAN =
            List.of(EstadoCita.CONFIRMADA, EstadoCita.EN_CAMINO, EstadoCita.EN_PROCESO);

    private final ResolverTecnicoAutenticado resolverTecnico;
    private final ConsultaCitasDelTecnicoPort consultaCitasPort;
    private final BloqueIndisponibilidadRepositoryPort bloquesExistentesPort;
    private final GestionBloquesPort gestionBloquesPort;
    private final RegistroLogPort registroLogPort;
    private final Clock reloj;

    public CrearBloqueUseCaseImpl(ResolverTecnicoAutenticado resolverTecnico,
                                  ConsultaCitasDelTecnicoPort consultaCitasPort,
                                  BloqueIndisponibilidadRepositoryPort bloquesExistentesPort,
                                  GestionBloquesPort gestionBloquesPort,
                                  RegistroLogPort registroLogPort,
                                  Clock reloj) {
        this.resolverTecnico = resolverTecnico;
        this.consultaCitasPort = consultaCitasPort;
        this.bloquesExistentesPort = bloquesExistentesPort;
        this.gestionBloquesPort = gestionBloquesPort;
        this.registroLogPort = registroLogPort;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public BloqueIndisponibilidad crear(String correoTecnico, LocalDateTime inicio, LocalDateTime fin, String motivo) {
        if (!fin.isAfter(inicio)) {
            throw new BloqueInvalidoException("La hora de fin debe ser posterior a la de inicio");
        }
        if (inicio.isBefore(LocalDateTime.now(reloj))) {
            throw new BloqueInvalidoException("El bloque no puede empezar en el pasado");
        }

        Tecnico tecnico = resolverTecnico.resolverBloqueando(correoTecnico);

        List<Cita> citasQueCruzan = consultaCitasPort
                .buscarPorEstadosEntre(tecnico.getId(), ESTADOS_QUE_OCUPAN, inicio, fin);
        if (!citasQueCruzan.isEmpty()) {
            throw new BloqueSolapadoException("El bloque cruza la cita " + citasQueCruzan.get(0).getId()
                    + " que ya está agendada; no se puede bloquear ese rango");
        }
        if (!bloquesExistentesPort.buscarDelTecnicoEntre(tecnico.getId(), inicio, fin).isEmpty()) {
            throw new BloqueSolapadoException("El bloque cruza otro bloque de indisponibilidad tuyo");
        }

        BloqueIndisponibilidad guardado = gestionBloquesPort.guardar(
                new BloqueIndisponibilidad(null, tecnico.getId(), inicio, fin, motivo));
        registroLogPort.registrar(NivelLog.INFO, TipoLog.NEGOCIO, "CrearBloqueUseCaseImpl", correoTecnico,
                "Bloque de indisponibilidad creado con id " + guardado.getId());
        return guardado;
    }
}
