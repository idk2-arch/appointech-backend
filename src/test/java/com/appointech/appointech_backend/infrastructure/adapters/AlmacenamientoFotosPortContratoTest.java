package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.FotoAlmacenada;
import com.appointech.appointech_backend.domain.ports.out.AlmacenamientoFotosPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contrato que toda implementación de AlmacenamientoFotosPort debe cumplir (principio de Liskov). */
abstract class AlmacenamientoFotosPortContratoTest {

    private static final String CLAVE = "11111111-2222-3333-4444-555555555555";

    private AlmacenamientoFotosPort almacenamiento;

    protected abstract AlmacenamientoFotosPort crear();

    @BeforeEach
    void preparar() {
        almacenamiento = crear();
    }

    @Test
    void loGuardadoSePuedeLeerConSuTipoDeContenido() {
        almacenamiento.guardar(CLAVE, new byte[]{1, 2, 3}, "image/png");

        FotoAlmacenada foto = almacenamiento.obtener(CLAVE).orElseThrow();

        assertArrayEquals(new byte[]{1, 2, 3}, foto.contenido());
        assertEquals("image/png", foto.tipoContenido());
    }

    @Test
    void guardarSobreLaMismaClaveReemplazaElContenido() {
        almacenamiento.guardar(CLAVE, new byte[]{1}, "image/png");
        almacenamiento.guardar(CLAVE, new byte[]{9, 9}, "image/jpeg");

        FotoAlmacenada foto = almacenamiento.obtener(CLAVE).orElseThrow();

        assertArrayEquals(new byte[]{9, 9}, foto.contenido());
        assertEquals("image/jpeg", foto.tipoContenido());
    }

    @Test
    void unaClaveInexistenteDevuelveVacio() {
        assertEquals(Optional.empty(), almacenamiento.obtener(CLAVE));
    }

    @Test
    void despuesDeEliminarYaNoExisteYEliminarDeNuevoNoFalla() {
        almacenamiento.guardar(CLAVE, new byte[]{1}, "image/webp");

        almacenamiento.eliminar(CLAVE);

        assertTrue(almacenamiento.obtener(CLAVE).isEmpty());
        assertDoesNotThrow(() -> almacenamiento.eliminar(CLAVE));
    }
}
