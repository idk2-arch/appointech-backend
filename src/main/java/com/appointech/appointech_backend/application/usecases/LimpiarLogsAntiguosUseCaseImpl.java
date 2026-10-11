package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.ports.in.LimpiarLogsAntiguosUseCase;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LimpiarLogsAntiguosUseCaseImpl implements LimpiarLogsAntiguosUseCase {

    private final RegistroLogPort registroLogPort;

    public LimpiarLogsAntiguosUseCaseImpl(RegistroLogPort registroLogPort) {
        this.registroLogPort = registroLogPort;
    }

    @Override
    public int limpiar(int diasDeRetencion) {
        int eliminados = registroLogPort.eliminarAnterioresA(LocalDateTime.now().minusDays(diasDeRetencion));
        registroLogPort.registrar(NivelLog.INFO, TipoLog.SISTEMA, "LimpiarLogsAntiguosUseCaseImpl", null,
                "Limpieza de logs: " + eliminados + " registros anteriores a " + diasDeRetencion + " días");
        return eliminados;
    }
}
