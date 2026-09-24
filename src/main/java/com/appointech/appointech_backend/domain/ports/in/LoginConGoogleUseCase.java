package com.appointech.appointech_backend.domain.ports.in;

public interface LoginConGoogleUseCase {

    ResultadoLogin ejecutar(String idToken);

    record ResultadoLogin(String token, String rol, String nombre, String correo, boolean perfilCompleto) {
    }
}