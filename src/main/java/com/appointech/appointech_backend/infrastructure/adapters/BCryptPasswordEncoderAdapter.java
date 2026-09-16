package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.ports.out.PasswordEncoderPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordEncoderAdapter implements PasswordEncoderPort {

    private final PasswordEncoder passwordEncoder;

    public BCryptPasswordEncoderAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String encriptar(String contrasenaPlana) {
        return passwordEncoder.encode(contrasenaPlana);
    }

    @Override
    public boolean coincide(String contrasenaPlana, String contrasenaEncriptada) {
        return passwordEncoder.matches(contrasenaPlana, contrasenaEncriptada);
    }
}