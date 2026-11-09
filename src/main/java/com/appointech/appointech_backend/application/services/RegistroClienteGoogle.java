package com.appointech.appointech_backend.application.services;

import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.GoogleTokenVerifierPort.GoogleUser;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Registra de forma atómica un cliente nuevo que llega con Google: usuario y fila de cliente
 * se guardan juntos o no se guarda nada. Es un servicio aparte para que la transacción cubra
 * solo el registro y no la verificación del token con Google.
 */
@Service
public class RegistroClienteGoogle {

    private final UsuarioRepositoryPort usuarioRepository;
    private final ClienteRepositoryPort clienteRepository;

    public RegistroClienteGoogle(UsuarioRepositoryPort usuarioRepository, ClienteRepositoryPort clienteRepository) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
    }

    /**
     * Crea el usuario CLIENTE con los datos de Google y su fila en la tabla cliente (sin dirección;
     * se completa con PATCH /api/auth/completar-perfil). Queda SIN contraseña: solo entra con Google.
     */
    @Transactional
    public Usuario registrar(GoogleUser googleUser) {
        Usuario nuevo = new Usuario();
        nuevo.setNombre(googleUser.nombre());
        nuevo.setApellido(googleUser.apellido());
        nuevo.setCorreo(googleUser.correo());
        nuevo.setContrasena(null);
        nuevo.setRol(RolUsuario.CLIENTE);
        nuevo.setActivo(true);
        nuevo.setCreadoEn(LocalDateTime.now());
        Usuario usuarioGuardado = usuarioRepository.guardar(nuevo);

        Cliente cliente = new Cliente();
        cliente.setUsuario(usuarioGuardado);
        clienteRepository.guardar(cliente);

        return usuarioGuardado;
    }
}
