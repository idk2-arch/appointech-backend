package com.appointech.appointech_backend.domain.ports.in;

public interface EliminarBloqueUseCase {
    void eliminar(String correoTecnico, Long bloqueId);
}
