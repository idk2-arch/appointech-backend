package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.RegistroLog;
import com.appointech.appointech_backend.domain.ports.in.ConsultarLogsUseCase;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConsultarLogsUseCaseImpl implements ConsultarLogsUseCase {

    private static final int LIMITE_MAXIMO = 500;

    private final RegistroLogPort registroLogPort;

    public ConsultarLogsUseCaseImpl(RegistroLogPort registroLogPort) {
        this.registroLogPort = registroLogPort;
    }

    @Override
    public List<RegistroLog> consultar(String nivel, String tipo, int limite) {
        int limiteAjustado = Math.max(1, Math.min(limite, LIMITE_MAXIMO));
        String nivelNormalizado = nivel == null || nivel.isBlank() ? null : nivel.trim().toUpperCase();
        String tipoNormalizado = tipo == null || tipo.isBlank() ? null : tipo.trim().toUpperCase();
        return registroLogPort.listar(nivelNormalizado, tipoNormalizado, limiteAjustado);
    }
}
