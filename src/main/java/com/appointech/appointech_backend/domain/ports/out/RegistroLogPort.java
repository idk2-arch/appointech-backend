package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.RegistroLog;
import com.appointech.appointech_backend.domain.models.TipoLog;

import java.time.LocalDateTime;
import java.util.List;

public interface RegistroLogPort {
    void registrar(NivelLog nivel, TipoLog tipo, String origen, String usuario, String mensaje);
    List<RegistroLog> listar(String nivel, String tipo, int limite);
    int eliminarAnterioresA(LocalDateTime fechaLimite);
}
