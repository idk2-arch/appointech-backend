package com.appointech.appointech_backend.application.services;

import com.appointech.appointech_backend.domain.exception.AlmacenamientoNoDisponibleException;
import com.appointech.appointech_backend.domain.models.ImagenValidada;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.models.ValidadorImagenFoto;
import com.appointech.appointech_backend.domain.ports.out.AlmacenamientoFotosPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Coordina validar la imagen, guardarla en el almacenamiento y actualizar la clave del usuario.
 * La clave del objeto es un UUID aleatorio: nunca se deriva de datos del usuario.
 */
@Component
public class GestorFotoUsuario {

    private static final String ORIGEN = "GestorFotoUsuario";

    private final AlmacenamientoFotosPort almacenamiento;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final RegistroLogPort registroLogPort;

    public GestorFotoUsuario(AlmacenamientoFotosPort almacenamiento,
                             UsuarioRepositoryPort usuarioRepositoryPort,
                             RegistroLogPort registroLogPort) {
        this.almacenamiento = almacenamiento;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.registroLogPort = registroLogPort;
    }

    /** Valida y guarda la imagen, apunta el usuario a la clave nueva y borra el objeto anterior. */
    public String reemplazar(Usuario usuario, String imagenBase64) {
        ImagenValidada imagen = ValidadorImagenFoto.validar(imagenBase64);
        String clave = UUID.randomUUID().toString();
        almacenamiento.guardar(clave, imagen.contenido(), imagen.tipo().tipoContenido());

        String anterior = usuario.getFotoClave();
        usuario.setFotoClave(clave);
        guardarUsuarioOLimpiar(usuario, clave);
        eliminarObjeto(anterior);
        return clave;
    }

    /** Quita la foto del usuario y borra el objeto; no hace nada si no tenía. */
    public void eliminar(Usuario usuario) {
        String anterior = usuario.getFotoClave();
        if (anterior == null) {
            return;
        }
        usuario.setFotoClave(null);
        usuarioRepositoryPort.guardar(usuario);
        eliminarObjeto(anterior);
    }

    private void guardarUsuarioOLimpiar(Usuario usuario, String claveNueva) {
        try {
            usuarioRepositoryPort.guardar(usuario);
        } catch (RuntimeException ex) {
            eliminarObjeto(claveNueva);
            throw ex;
        }
    }

    /** Un objeto huérfano no debe tumbar la operación: se avisa en el registro de eventos y se sigue. */
    private void eliminarObjeto(String clave) {
        if (clave == null) {
            return;
        }
        try {
            almacenamiento.eliminar(clave);
        } catch (AlmacenamientoNoDisponibleException ex) {
            registroLogPort.registrar(NivelLog.WARN, TipoLog.INTEGRACION, ORIGEN, null,
                    "No se pudo eliminar una foto anterior del almacenamiento; quedó un objeto huérfano (" + ex.getMessage() + ")");
        }
    }
}
