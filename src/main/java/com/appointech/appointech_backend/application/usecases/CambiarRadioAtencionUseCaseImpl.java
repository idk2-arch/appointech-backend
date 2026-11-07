package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.TecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.ports.in.CambiarRadioAtencionUseCase;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Caso de uso: el administrador fija hasta qué distancia de su base atiende cada técnico. */
@Service
public class CambiarRadioAtencionUseCaseImpl implements CambiarRadioAtencionUseCase {

    private final TecnicoRepositoryPort tecnicoRepositoryPort;
    private final RegistroLogPort registroLogPort;

    public CambiarRadioAtencionUseCaseImpl(TecnicoRepositoryPort tecnicoRepositoryPort, RegistroLogPort registroLogPort) {
        this.tecnicoRepositoryPort = tecnicoRepositoryPort;
        this.registroLogPort = registroLogPort;
    }

    @Override
    @Transactional
    public Tecnico cambiar(String correoAdministrador, Long tecnicoId, Integer radioMaximoKm) {
        Tecnico tecnico = tecnicoRepositoryPort.buscarPorId(tecnicoId)
                .orElseThrow(() -> new TecnicoNoEncontradoException(tecnicoId));
        tecnico.cambiarRadioCobertura(radioMaximoKm);
        Tecnico guardado = tecnicoRepositoryPort.guardar(tecnico);
        registroLogPort.registrar(NivelLog.INFO, TipoLog.AUDITORIA, "CambiarRadioAtencionUseCaseImpl", correoAdministrador,
                "Radio de atención del técnico " + tecnicoId + " fijado en " + radioMaximoKm + " km");
        return guardado;
    }
}
