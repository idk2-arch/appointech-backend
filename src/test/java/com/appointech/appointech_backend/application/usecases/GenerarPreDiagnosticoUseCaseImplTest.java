package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.PreDiagnosticoInvalidoException;
import com.appointech.appointech_backend.domain.models.PreDiagnostico;
import com.appointech.appointech_backend.domain.ports.out.PreDiagnosticoPort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Puerto falso: el caso de uso solo valida y delega, sin red ni Groq. */
class GenerarPreDiagnosticoUseCaseImplTest {

    private final List<String> llamadas = new ArrayList<>();
    private PreDiagnostico respuesta = PreDiagnostico.conSugerencia("Revisa el sifón");

    private final PreDiagnosticoPort puerto = (descripcion, especialidad) -> {
        llamadas.add(descripcion + "|" + especialidad);
        return respuesta;
    };
    private final GenerarPreDiagnosticoUseCaseImpl casoDeUso = new GenerarPreDiagnosticoUseCaseImpl(puerto);

    @Test
    void devuelveLaSugerenciaDelPuertoRecortandoEspacios() {
        PreDiagnostico resultado = casoDeUso.generar("  Fuga bajo el lavaplatos  ", " Plomería ");

        assertTrue(resultado.disponible());
        assertEquals("Revisa el sifón", resultado.sugerencia());
        assertEquals(List.of("Fuga bajo el lavaplatos|Plomería"), llamadas);
    }

    @Test
    void laEspecialidadVaciaOAusenteSePasaComoNula() {
        casoDeUso.generar("Fuga", null);
        casoDeUso.generar("Fuga", "   ");

        assertEquals(List.of("Fuga|null", "Fuga|null"), llamadas);
    }

    @Test
    void siElPuertoDiceQueNoEstaDisponibleSeDevuelveEseResultado() {
        respuesta = PreDiagnostico.noDisponible();

        PreDiagnostico resultado = casoDeUso.generar("Fuga", null);

        assertFalse(resultado.disponible());
        assertEquals(PreDiagnostico.MENSAJE_NO_DISPONIBLE, resultado.sugerencia());
    }

    @Test
    void sinDescripcionONoSeLlamaAlPuerto() {
        assertThrows(PreDiagnosticoInvalidoException.class, () -> casoDeUso.generar(null, null));
        assertThrows(PreDiagnosticoInvalidoException.class, () -> casoDeUso.generar("   ", null));
        assertTrue(llamadas.isEmpty());
    }

    @Test
    void rechazaDescripcionYEspecialidadDemasiadoLargas() {
        assertEquals("La descripción del problema no puede superar 1000 caracteres",
                assertThrows(PreDiagnosticoInvalidoException.class,
                        () -> casoDeUso.generar("a".repeat(1001), null)).getMessage());
        assertEquals("La especialidad no puede superar 100 caracteres",
                assertThrows(PreDiagnosticoInvalidoException.class,
                        () -> casoDeUso.generar("Fuga", "e".repeat(101))).getMessage());
        assertTrue(llamadas.isEmpty());
    }

    @Test
    void aceptaLosLimitesExactos() {
        casoDeUso.generar("a".repeat(1000), "e".repeat(100));

        assertEquals(1, llamadas.size());
    }
}
