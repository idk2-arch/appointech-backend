package com.appointech.appointech_backend.domain.ports.in;

public interface AutenticarUsuarioUseCase {
    String autenticar(String correo, String contrasena); // devuelve el token JWT
}