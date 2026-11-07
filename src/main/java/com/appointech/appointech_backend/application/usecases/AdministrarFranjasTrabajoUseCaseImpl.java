package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.ReglasFranjasTrabajo;
import com.appointech.appointech_backend.domain.exception.FranjaTrabajoNoEncontradaException;
import com.appointech.appointech_backend.domain.exception.TecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.UsuarioNoEncontradoException;
import com.appointech.appointech_backend.domain.models.FranjaTrabajo;
import com.appointech.appointech_backend.domain.models.FranjaTrabajoConOcupacion;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.ports.in.AdministrarFranjasTrabajoUseCase;
import com.appointech.appointech_backend.domain.ports.out.FranjaTrabajoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Caso de uso: el administrador define cuándo trabaja cada técnico, fecha por fecha. Al crear o cambiar una franja se
 * bloquea la fila del técnico para que dos peticiones a la vez no creen franjas que se crucen.
 */
@Service
public class AdministrarFranjasTrabajoUseCaseImpl implements AdministrarFranjasTrabajoUseCase {

    private final TecnicoRepositoryPort tecnicoRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final FranjaTrabajoRepositoryPort franjaRepositoryPort;
    private final ReglasFranjasTrabajo reglas;
    private final RegistroLogPort registroLogPort;

    public AdministrarFranjasTrabajoUseCaseImpl(TecnicoRepositoryPort tecnicoRepositoryPort,
                                                UsuarioRepositoryPort usuarioRepositoryPort,
                                                FranjaTrabajoRepositoryPort franjaRepositoryPort,
                                                ReglasFranjasTrabajo reglas, RegistroLogPort registroLogPort) {
        this.tecnicoRepositoryPort = tecnicoRepositoryPort;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.franjaRepositoryPort = franjaRepositoryPort;
        this.reglas = reglas;
        this.registroLogPort = registroLogPort;
    }

    @Override
    @Transactional
    public FranjaTrabajoConOcupacion crear(String correoAdministrador, Long tecnicoId, LocalDate fecha,
                                           LocalTime horaInicio, LocalTime horaFin) {
        exigirTecnicoBloqueando(tecnicoId);
        Long administradorId = usuarioRepositoryPort.buscarPorCorreo(correoAdministrador)
                .orElseThrow(() -> new UsuarioNoEncontradoException(correoAdministrador)).getId();
        FranjaTrabajo nueva = FranjaTrabajo.validada(null, tecnicoId, fecha, horaInicio, horaFin,
                reglas.duracionBloqueMinutos(), reglas.ahora(), administradorId, reglas.ahora());
        reglas.exigirSinCruces(nueva);

        FranjaTrabajo guardada = franjaRepositoryPort.guardar(nueva);
        registrar(correoAdministrador, "creada", guardada);
        return reglas.conOcupacion(guardada);
    }

    @Override
    @Transactional
    public FranjaTrabajoConOcupacion actualizar(String correoAdministrador, Long tecnicoId, Long franjaId,
                                                LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        exigirTecnicoBloqueando(tecnicoId);
        FranjaTrabajo actual = franjaDelTecnico(tecnicoId, franjaId);
        reglas.exigirSinCitas(actual, "cambiar");
        FranjaTrabajo cambiada = FranjaTrabajo.validada(franjaId, tecnicoId, fecha, horaInicio, horaFin,
                reglas.duracionBloqueMinutos(), reglas.ahora(), actual.creadaPorId(), actual.creadaEn());
        reglas.exigirSinCruces(cambiada);

        FranjaTrabajo guardada = franjaRepositoryPort.guardar(cambiada);
        registrar(correoAdministrador, "cambiada", guardada);
        return reglas.conOcupacion(guardada);
    }

    @Override
    @Transactional
    public void eliminar(String correoAdministrador, Long tecnicoId, Long franjaId) {
        exigirTecnicoBloqueando(tecnicoId);
        FranjaTrabajo actual = franjaDelTecnico(tecnicoId, franjaId);
        reglas.exigirSinCitas(actual, "borrar");
        franjaRepositoryPort.eliminar(franjaId);
        registrar(correoAdministrador, "borrada", actual);
    }

    private void exigirTecnicoBloqueando(Long tecnicoId) {
        tecnicoRepositoryPort.buscarPorIdBloqueando(tecnicoId).orElseThrow(() -> new TecnicoNoEncontradoException(tecnicoId));
    }

    private FranjaTrabajo franjaDelTecnico(Long tecnicoId, Long franjaId) {
        return franjaRepositoryPort.buscarDelTecnico(franjaId, tecnicoId)
                .orElseThrow(() -> new FranjaTrabajoNoEncontradaException(franjaId));
    }

    private void registrar(String correoAdministrador, String accion, FranjaTrabajo franja) {
        registroLogPort.registrar(NivelLog.INFO, TipoLog.AUDITORIA, "AdministrarFranjasTrabajoUseCaseImpl",
                correoAdministrador, "Franja de trabajo " + accion + " del técnico " + franja.tecnicoId() + ": "
                        + franja.descripcion());
    }
}
