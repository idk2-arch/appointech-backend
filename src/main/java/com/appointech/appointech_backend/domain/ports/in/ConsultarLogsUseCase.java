package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.RegistroLog;

import java.util.List;

public interface ConsultarLogsUseCase {
    List<RegistroLog> consultar(String nivel, String tipo, int limite);
}
