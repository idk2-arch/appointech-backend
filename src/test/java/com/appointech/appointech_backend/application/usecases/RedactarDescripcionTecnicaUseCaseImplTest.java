package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.PreDiagnosticoInvalidoException;
import com.appointech.appointech_backend.domain.models.RedaccionTecnica;
import com.appointech.appointech_backend.domain.ports.out.RedactorTecnicoPort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Puerto falso: el caso de uso solo valida y delega. */
class RedactarDescripcionTecnicaUseCaseImplTest {

    private final List<String> llamadas = new ArrayList<>();
    private RedaccionTecnica respuesta = RedaccionTecnica.conTexto("Fuga en sifón");

    private final RedactorTecnicoPort puerto = (descripcion, especialidad) -> {
        llamadas.add(descripcion + "|" + especialidad);
        return respuesta;
    };
    private final RedactarDescripcionTecnicaUseCaseImpl casoDeUso = new RedactarDescripcionTecnicaUseCaseImpl(puerto);

    @Test
    void devuelveElTextoDelPuertoRecortandoEspacios() {
        RedaccionTecnica resultado = casoDeUso.redactar("  se sale el agua  ", " Plomería ");

        assertTrue(resultado.disponible());
        assertEquals("Fuga en sifón", resultado.textoTecnico());
        assertEquals(List.of("se sale el agua|Plomería"), llamadas);
    }

    @Test
    void laEspecialidadVaciaOAusenteSePasaComoNula() {
        casoDeUso.redactar("falla", null);
        casoDeUso.redactar("falla", "  ");

        assertEquals(List.of("falla|null", "falla|null"), llamadas);
    }

    @Test
    void siElPuertoNoEstaDisponibleSeDevuelveEseResultado() {
        respuesta = RedaccionTecnica.noDisponible();

        RedaccionTecnica resultado = casoDeUso.redactar("falla", null);

        assertFalse(resultado.disponible());
        assertEquals(RedaccionTecnica.MENSAJE_NO_DISPONIBLE, resultado.textoTecnico());
    }

    @Test
    void rechazaEntradasInvalidasSinLlamarAlPuerto() {
        assertEquals("La descripción del problema es obligatoria",
                assertThrows(PreDiagnosticoInvalidoException.class, () -> casoDeUso.redactar(null, null)).getMessage());
        assertThrows(PreDiagnosticoInvalidoException.class, () -> casoDeUso.redactar("   ", null));
        assertEquals("La descripción del problema no puede superar 1000 caracteres",
                assertThrows(PreDiagnosticoInvalidoException.class,
                        () -> casoDeUso.redactar("a".repeat(1001), null)).getMessage());
        assertEquals("La especialidad no puede superar 100 caracteres",
                assertThrows(PreDiagnosticoInvalidoException.class,
                        () -> casoDeUso.redactar("falla", "e".repeat(101))).getMessage());
        assertTrue(llamadas.isEmpty());
    }

    @Test
    void aceptaLosLimitesExactos() {
        casoDeUso.redactar("a".repeat(1000), "e".repeat(100));

        assertEquals(1, llamadas.size());
    }
}
