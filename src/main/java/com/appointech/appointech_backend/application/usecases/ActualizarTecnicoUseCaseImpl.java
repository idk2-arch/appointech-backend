package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.CorreoYaRegistradoException;
import com.appointech.appointech_backend.domain.exception.DocumentoYaRegistradoException;
import com.appointech.appointech_backend.domain.exception.TecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.models.Coordenadas;
import com.appointech.appointech_backend.domain.models.DatosActualizacionTecnico;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.ActualizarTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.in.AsignarEspecialidadesTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActualizarTecnicoUseCaseImpl implements ActualizarTecnicoUseCase {

    private final TecnicoRepositoryPort tecnicoRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final AsignarEspecialidadesTecnicoUseCase asignarEspecialidadesTecnicoUseCase;

    public ActualizarTecnicoUseCaseImpl(TecnicoRepositoryPort tecnicoRepositoryPort,
                                        UsuarioRepositoryPort usuarioRepositoryPort,
                                        AsignarEspecialidadesTecnicoUseCase asignarEspecialidadesTecnicoUseCase) {
        this.tecnicoRepositoryPort = tecnicoRepositoryPort;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.asignarEspecialidadesTecnicoUseCase = asignarEspecialidadesTecnicoUseCase;
    }

    @Override
    @Transactional
    public Tecnico actualizar(Long tecnicoId, DatosActualizacionTecnico datos) {
        Tecnico tecnico = tecnicoRepositoryPort.buscarPorId(tecnicoId)
                .orElseThrow(() -> new TecnicoNoEncontradoException(tecnicoId));
        Usuario usuario = tecnico.getUsuario();

        Coordenadas base = Coordenadas.opcional(datos.latitudBase(), datos.longitudBase());
        validarCorreoDisponible(usuario, datos.correo());
        validarDocumentoDisponible(usuario, datos.numeroDocumento());

        if (datos.nombre() != null) usuario.setNombre(datos.nombre());
        if (datos.apellido() != null) usuario.setApellido(datos.apellido());
        if (datos.correo() != null) usuario.setCorreo(datos.correo());
        if (datos.telefono() != null) usuario.setTelefono(datos.telefono());
        if (datos.tipoDocumento() != null) usuario.setTipoDocumento(datos.tipoDocumento());
        if (datos.numeroDocumento() != null) usuario.setNumeroDocumento(datos.numeroDocumento());
        usuarioRepositoryPort.guardar(usuario);

        if (datos.direccionBase() != null) tecnico.setDireccionBase(datos.direccionBase());
        if (datos.radioCoberturaKm() != null) tecnico.setRadioCoberturaKm(datos.radioCoberturaKm());
        if (base != null) {
            tecnico.setLatitudBase(base.latitud());
            tecnico.setLongitudBase(base.longitud());
        }
        Tecnico guardado = tecnicoRepositoryPort.guardar(tecnico);

        if (datos.especialidadIds() == null) {
            return guardado;
        }
        return asignarEspecialidadesTecnicoUseCase.asignar(guardado.getId(),
                datos.especialidadIds().stream().distinct().toList());
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
}
