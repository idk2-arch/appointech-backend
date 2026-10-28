package com.appointech.appointech_backend.domain.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerfilUtilTest {

    private Usuario usuario(RolUsuario rol, String telefono, TipoDocumento tipo, String numero) {
        Usuario u = new Usuario();
        u.setRol(rol);
        u.setTelefono(telefono);
        u.setTipoDocumento(tipo);
        u.setNumeroDocumento(numero);
        return u;
    }

    private Cliente cliente(String direccion) {
        Cliente c = new Cliente();
        c.setDireccion(direccion);
        return c;
    }

    @Test
    void tecnicoYAdminSoloNecesitanDatosBase() {
        assertTrue(PerfilUtil.estaCompleto(usuario(RolUsuario.TECNICO, "300", TipoDocumento.CC, "1"), null));
        assertTrue(PerfilUtil.estaCompleto(usuario(RolUsuario.ADMINISTRADOR, "300", TipoDocumento.CE, "1"), null));
    }

    @Test
    void clienteNecesitaDireccion() {
        Usuario u = usuario(RolUsuario.CLIENTE, "300", TipoDocumento.CC, "1");
        assertTrue(PerfilUtil.estaCompleto(u, cliente("Calle 1")));
        assertFalse(PerfilUtil.estaCompleto(u, cliente(null)));
        assertFalse(PerfilUtil.estaCompleto(u, cliente("   ")));
        assertFalse(PerfilUtil.estaCompleto(u, null));
    }

    @Test
    void faltaCualquierDatoBaseEsIncompleto() {
        Cliente c = cliente("Calle 1");
        assertFalse(PerfilUtil.estaCompleto(usuario(RolUsuario.CLIENTE, null, TipoDocumento.CC, "1"), c));
        assertFalse(PerfilUtil.estaCompleto(usuario(RolUsuario.CLIENTE, " ", TipoDocumento.CC, "1"), c));
        assertFalse(PerfilUtil.estaCompleto(usuario(RolUsuario.CLIENTE, "300", null, "1"), c));
        assertFalse(PerfilUtil.estaCompleto(usuario(RolUsuario.CLIENTE, "300", TipoDocumento.CC, null), c));
        assertFalse(PerfilUtil.estaCompleto(usuario(RolUsuario.TECNICO, "300", TipoDocumento.CC, ""), null));
    }
}
