package com.appointech.appointech_backend.domain.models;

public class PerfilUtil {

    private PerfilUtil() {
    }

    public static boolean estaCompleto(Usuario usuario, Cliente cliente) {
        boolean datosBaseCompletos = usuario.getTelefono() != null && !usuario.getTelefono().isBlank()
                && usuario.getTipoDocumento() != null
                && usuario.getNumeroDocumento() != null && !usuario.getNumeroDocumento().isBlank();

        if (usuario.getRol() == RolUsuario.CLIENTE) {
            boolean direccionCompleta = cliente != null
                    && cliente.getDireccion() != null
                    && !cliente.getDireccion().isBlank();
            return datosBaseCompletos && direccionCompleta;
        }

        return datosBaseCompletos;
    }
}