package com.appointech.appointech_backend.domain.ports.in;

public interface AutenticarUsuarioUseCase {
    ResultadoAutenticacion autenticar(String correo, String contrasena);

    record ResultadoAutenticacion(String token, boolean perfilCompleto) {
    }
}