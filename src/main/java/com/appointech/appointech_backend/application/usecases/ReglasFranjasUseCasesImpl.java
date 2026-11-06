package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.ConfiguracionFranjas;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.ReglasFranjas;
import com.appointech.appointech_backend.domain.models.ResultadoActualizacionReglas;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.ports.in.ActualizarReglasFranjasUseCase;
import com.appointech.appointech_backend.domain.ports.in.ObtenerReglasFranjasUseCase;
import com.appointech.appointech_backend.domain.ports.out.ConfiguracionFranjasRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

/**
 * Casos de uso: consultar y editar las reglas de franjas horarias (radio, duración del bloque y jornada).
 * <p>Historia: RF17 - Configurar reglas de franjas horarias. Las reglas se leen en cada cálculo (RF19), por lo
 * que aplican desde el siguiente cálculo de disponibilidad.
 */
@Service
public class ReglasFranjasUseCasesImpl implements ObtenerReglasFranjasUseCase, ActualizarReglasFranjasUseCase {

    private final ConfiguracionFranjasRepositoryPort configuracionPort;
    private final RegistroLogPort registroLogPort;

    public ReglasFranjasUseCasesImpl(ConfiguracionFranjasRepositoryPort configuracionPort,
                                     RegistroLogPort registroLogPort) {
        this.configuracionPort = configuracionPort;
        this.registroLogPort = registroLogPort;
    }

    @Override
    @Transactional(readOnly = true)
    public ReglasFranjas obtener() {
        return ReglasFranjas.de(configuracionActual());
    }

    @Override
    @Transactional
    public ResultadoActualizacionReglas actualizar(String correoAdministrador, Integer radioMaximoKm,
                                                   Integer duracionBloqueMinutos, LocalTime horaInicioJornada,
                                                   LocalTime horaFinJornada) {
        ConfiguracionFranjas configuracion = configuracionActual();
        boolean invalidaFranjas = configuracion.cambiaranFranjasOfrecidas(
                duracionBloqueMinutos, horaInicioJornada, horaFinJornada);

        // Las validaciones corren antes de guardar: si alguna falla no se persiste nada.
        configuracion.cambiarRadioMaximo(radioMaximoKm);
        configuracion.cambiarReglasDeFranjas(duracionBloqueMinutos, horaInicioJornada, horaFinJornada);
        ReglasFranjas guardadas = ReglasFranjas.de(configuracionPort.guardar(configuracion));

        registroLogPort.registrar(NivelLog.INFO, TipoLog.AUDITORIA, "ReglasFranjasUseCasesImpl", correoAdministrador,
                "Reglas de franjas actualizadas: radio " + guardadas.radioMaximoKm() + " km, bloque "
                        + guardadas.duracionBloqueMinutos() + " min, jornada " + guardadas.horaInicioJornada()
                        + "-" + guardadas.horaFinJornada());
        return new ResultadoActualizacionReglas(guardadas, invalidaFranjas);
    }

    private ConfiguracionFranjas configuracionActual() {
        return configuracionPort.obtenerActual().orElseGet(ConfiguracionFranjas::porDefecto);
    }
}
