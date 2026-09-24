package com.appointech.appointech_backend.domain.ports.out;

import java.util.Optional;

/**
 * Puerto de salida: verifica un ID token emitido por Google.
 * El dominio no conoce la librería de Google; solo recibe
 * los datos del usuario ya validados.
 */
public interface GoogleTokenVerifierPort {

    Optional<GoogleUser> verificar(String idToken);

    record GoogleUser(String googleId, String correo, String nombre, String apellido) {}
}