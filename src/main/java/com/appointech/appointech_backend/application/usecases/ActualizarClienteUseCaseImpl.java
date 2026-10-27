package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.ClientePorIdNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.CorreoYaRegistradoException;
import com.appointech.appointech_backend.domain.exception.DocumentoYaRegistradoException;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.Coordenadas;
import com.appointech.appointech_backend.domain.models.DatosActualizacionCliente;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.ActualizarClienteUseCase;
import com.appointech.appointech_backend.domain.ports.out.ClienteRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActualizarClienteUseCaseImpl implements ActualizarClienteUseCase {

    private final ClienteRepositoryPort clienteRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;

    public ActualizarClienteUseCaseImpl(ClienteRepositoryPort clienteRepositoryPort,
                                        UsuarioRepositoryPort usuarioRepositoryPort) {
        this.clienteRepositoryPort = clienteRepositoryPort;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
    }

    @Override
    @Transactional
    public Cliente actualizar(Long clienteId, DatosActualizacionCliente datos) {
        Cliente cliente = clienteRepositoryPort.buscarPorId(clienteId)
                .orElseThrow(() -> new ClientePorIdNoEncontradoException(clienteId));
        Usuario usuario = cliente.getUsuario();

        Coordenadas ubicacion = Coordenadas.opcional(datos.latitud(), datos.longitud());
        validarCorreoDisponible(usuario, datos.correo());
        validarDocumentoDisponible(usuario, datos.numeroDocumento());

        if (datos.nombre() != null) usuario.setNombre(datos.nombre());
        if (datos.apellido() != null) usuario.setApellido(datos.apellido());
        if (datos.correo() != null) usuario.setCorreo(datos.correo());
        if (datos.telefono() != null) usuario.setTelefono(datos.telefono());
        if (datos.tipoDocumento() != null) usuario.setTipoDocumento(datos.tipoDocumento());
        if (datos.numeroDocumento() != null) usuario.setNumeroDocumento(datos.numeroDocumento());
        usuarioRepositoryPort.guardar(usuario);

        if (datos.direccion() != null) cliente.setDireccion(datos.direccion());
        if (ubicacion != null) {
            cliente.setLatitud(ubicacion.latitud());
            cliente.setLongitud(ubicacion.longitud());
        }
        return clienteRepositoryPort.guardar(cliente);
    }

    private void validarCorreoDisponible(Usuario usuario, String correoNuevo) {
        if (correoNuevo == null || correoNuevo.equalsIgnoreCase(usuario.getCorreo())) {
            return;
        }
        usuarioRepositoryPort.buscarPorCorreo(correoNuevo)
                .filter(otro -> !otro.getId().equals(usuario.getId()))
                .ifPresent(otro -> {
                    throw new CorreoYaRegistradoException(correoNuevo);
                });
    }

    private void validarDocumentoDisponible(Usuario usuario, String documentoNuevo) {
        if (documentoNuevo == null || documentoNuevo.equals(usuario.getNumeroDocumento())) {
            return;
        }
        usuarioRepositoryPort.buscarPorNumeroDocumento(documentoNuevo)
                .filter(otro -> !otro.getId().equals(usuario.getId()))
                .ifPresent(otro -> {
                    throw new DocumentoYaRegistradoException(documentoNuevo);
                });
    }
}
