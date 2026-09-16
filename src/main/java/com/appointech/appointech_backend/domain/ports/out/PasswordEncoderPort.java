package com.appointech.appointech_backend.domain.ports.out;

public interface PasswordEncoderPort {
    String encriptar(String contrasenaPlana);
    boolean coincide(String contrasenaPlana, String contrasenaEncriptada);
}
