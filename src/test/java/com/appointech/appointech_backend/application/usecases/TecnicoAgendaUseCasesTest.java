package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.BloqueInvalidoException;
import com.appointech.appointech_backend.domain.exception.BloqueNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.BloqueSolapadoException;
import com.appointech.appointech_backend.domain.exception.CitaSinUbicacionException;
import com.appointech.appointech_backend.domain.exception.PerfilTecnicoNoEncontradoException;
import com.appointech.appointech_backend.domain.exception.UbicacionBaseNoConfiguradaException;
import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.CitaConCliente;
import com.appointech.appointech_backend.domain.models.Coordenadas;
import com.appointech.appointech_backend.domain.models.Distancia;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.ParadaRuta;
import com.appointech.appointech_backend.domain.models.RutaDelDia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static com.appointech.appointech_backend.application.usecases.AgendaTecnicoEscenario.AHORA;
import static com.appointech.appointech_backend.application.usecases.AgendaTecnicoEscenario.BASE;
import static com.appointech.appointech_backend.application.usecases.AgendaTecnicoEscenario.CORREO_OTRO;
import static com.appointech.appointech_backend.application.usecases.AgendaTecnicoEscenario.CORREO_TECNICO;
import static com.appointech.appointech_backend.application.usecases.AgendaTecnicoEscenario.HOY;
import static com.appointech.appointech_backend.application.usecases.AgendaTecnicoEscenario.MANANA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TecnicoAgendaUseCasesTest {

    private static final Coordenadas PUNTO_A = new Coordenadas(4.40, -74.30);
    private static final Coordenadas PUNTO_B = new Coordenadas(4.50, -74.20);
    private static final Coordenadas PUNTO_C = new Coordenadas(4.60, -74.10);

    private AgendaTecnicoEscenario mundo;
    private final LocalDateTime nueveManana = MANANA.atTime(9, 0);

    @BeforeEach
    void preparar() {
        mundo = new AgendaTecnicoEscenario();
    }

    private Cita confirmada(long id, LocalDateTime inicio, LocalDateTime fin, Coordenadas punto) {
        return mundo.agregarCita(id, 1L, EstadoCita.CONFIRMADA, inicio, fin, punto.latitud(), punto.longitud());
    }

    // ---------------------------------------------------------------- bloques (SCRUM-17)

    @Test
    void creaUnBloqueValidoYLoRegistraEnElLog() {
        BloqueIndisponibilidad bloque = mundo.crearBloque()
                .crear(CORREO_TECNICO, nueveManana, nueveManana.plusHours(2), "Vacuna");

        assertNotNull(bloque.getId());
        assertEquals(1L, bloque.getTecnicoId());
        assertEquals("Vacuna", bloque.getMotivo());
        assertEquals(1, mundo.bloques.size());
        assertEquals(1, mundo.logs.size());
    }

    @Test
    void rechazaBloqueConFinIgualOAnteriorAlInicio() {
        assertThrows(BloqueInvalidoException.class,
                () -> mundo.crearBloque().crear(CORREO_TECNICO, nueveManana, nueveManana, null));
        assertThrows(BloqueInvalidoException.class,
                () -> mundo.crearBloque().crear(CORREO_TECNICO, nueveManana, nueveManana.minusHours(1), null));
        assertTrue(mundo.bloques.isEmpty());
    }

    @Test
    void rechazaBloqueQueEmpiezaEnElPasado() {
        assertThrows(BloqueInvalidoException.class,
                () -> mundo.crearBloque().crear(CORREO_TECNICO, AHORA.minusHours(1), AHORA.plusHours(1), null));
    }

    @Test
    void rechazaBloqueQueCruzaUnaCitaConfirmadaConMensajeClaro() {
        confirmada(7L, nueveManana, nueveManana.plusHours(2), PUNTO_A);

        BloqueSolapadoException error = assertThrows(BloqueSolapadoException.class, () -> mundo.crearBloque()
                .crear(CORREO_TECNICO, nueveManana.plusHours(1), nueveManana.plusHours(3), null));

        assertTrue(error.getMessage().contains("7"));
        assertTrue(mundo.bloques.isEmpty());
    }

    @Test
    void tambienRechazaCruceConCitasEnCaminoOEnProceso() {
        mundo.agregarCita(8L, 1L, EstadoCita.EN_CAMINO, nueveManana, nueveManana.plusHours(1), 4.4, -74.3);
        assertThrows(BloqueSolapadoException.class, () -> mundo.crearBloque()
                .crear(CORREO_TECNICO, nueveManana, nueveManana.plusMinutes(30), null));
    }

    @Test
    void citasCanceladasFinalizadasOPendientesNoImpidenElBloque() {
        mundo.agregarCita(1L, 1L, EstadoCita.CANCELADA, nueveManana, nueveManana.plusHours(1), 4.4, -74.3);
        mundo.agregarCita(2L, 1L, EstadoCita.FINALIZADA, nueveManana, nueveManana.plusHours(1), 4.4, -74.3);
        mundo.agregarCita(3L, 1L, EstadoCita.PENDIENTE, nueveManana, nueveManana.plusHours(1), 4.4, -74.3);

        BloqueIndisponibilidad bloque = mundo.crearBloque()
                .crear(CORREO_TECNICO, nueveManana, nueveManana.plusHours(1), null);

        assertNotNull(bloque.getId());
    }

    @Test
    void unBloqueJustoAlLadoDeUnaCitaNoLaCruza() {
        confirmada(7L, nueveManana, nueveManana.plusHours(1), PUNTO_A);

        BloqueIndisponibilidad bloque = mundo.crearBloque()
                .crear(CORREO_TECNICO, nueveManana.plusHours(1), nueveManana.plusHours(2), null);

        assertNotNull(bloque.getId());
    }

    @Test
    void rechazaBloqueQueCruzaOtroBloquePropioPeroNoUnoAjeno() {
        mundo.crearBloque().crear(CORREO_OTRO, nueveManana, nueveManana.plusHours(2), null);
        mundo.crearBloque().crear(CORREO_TECNICO, nueveManana, nueveManana.plusHours(2), null);

        assertThrows(BloqueSolapadoException.class, () -> mundo.crearBloque()
                .crear(CORREO_TECNICO, nueveManana.plusHours(1), nueveManana.plusHours(3), null));
        assertEquals(2, mundo.bloques.size());
    }

    @Test
    void usuarioSinPerfilDeTecnicoRecibeNoEncontrado() {
        assertThrows(PerfilTecnicoNoEncontradoException.class, () -> mundo.crearBloque()
                .crear("nadie@correo.com", nueveManana, nueveManana.plusHours(1), null));
    }

    @Test
    void eliminaUnBloquePropio() {
        BloqueIndisponibilidad bloque = mundo.crearBloque()
                .crear(CORREO_TECNICO, nueveManana, nueveManana.plusHours(1), null);

        mundo.eliminarBloque().eliminar(CORREO_TECNICO, bloque.getId());

        assertTrue(mundo.bloques.isEmpty());
    }

    @Test
    void borrarUnBloqueAjenoOInexistenteEsNoEncontradoYNoLoBorra() {
        BloqueIndisponibilidad ajeno = mundo.crearBloque()
                .crear(CORREO_OTRO, nueveManana, nueveManana.plusHours(1), null);

        assertThrows(BloqueNoEncontradoException.class,
                () -> mundo.eliminarBloque().eliminar(CORREO_TECNICO, ajeno.getId()));
        assertThrows(BloqueNoEncontradoException.class,
                () -> mundo.eliminarBloque().eliminar(CORREO_TECNICO, 999L));
        assertEquals(1, mundo.bloques.size());
    }

    @Test
    void listaLosBloquesOrdenadosPorInicioDesdeHoyPorDefecto() {
        mundo.crearBloque().crear(CORREO_TECNICO, MANANA.atTime(15, 0), MANANA.atTime(16, 0), "tarde");
        mundo.crearBloque().crear(CORREO_TECNICO, MANANA.atTime(9, 0), MANANA.atTime(10, 0), "mañana");
        // Bloque de ayer que ya terminó: no debe salir por defecto.
        mundo.bloques.add(new BloqueIndisponibilidad(99L, 1L, HOY.minusDays(1).atTime(9, 0),
                HOY.minusDays(1).atTime(10, 0), "viejo"));

        List<BloqueIndisponibilidad> lista = mundo.listarBloques().listar(CORREO_TECNICO, null, null);

        assertEquals(List.of("mañana", "tarde"), lista.stream().map(BloqueIndisponibilidad::getMotivo).toList());
    }

    @Test
    void listaFiltraPorHastaInclusivoYRechazaRangoInvertido() {
        mundo.crearBloque().crear(CORREO_TECNICO, MANANA.atTime(9, 0), MANANA.atTime(10, 0), "uno");
        mundo.crearBloque().crear(CORREO_TECNICO, MANANA.plusDays(2).atTime(9, 0), MANANA.plusDays(2).atTime(10, 0), "dos");

        assertEquals(1, mundo.listarBloques().listar(CORREO_TECNICO, HOY, MANANA).size());
        assertThrows(BloqueInvalidoException.class,
                () -> mundo.listarBloques().listar(CORREO_TECNICO, MANANA, HOY));
    }

    // ---------------------------------------------------------------- mis citas

    @Test
    void consultaSoloCitasActivasDelTecnicoEnElDiaOrdenadasConDatosDelCliente() {
        confirmada(2L, MANANA.atTime(14, 0), MANANA.atTime(15, 0), PUNTO_B);
        confirmada(1L, MANANA.atTime(9, 0), MANANA.atTime(10, 0), PUNTO_A);
        mundo.agregarCita(3L, 1L, EstadoCita.CANCELADA, MANANA.atTime(11, 0), MANANA.atTime(12, 0), 4.4, -74.3);
        mundo.agregarCita(4L, 1L, EstadoCita.EN_PROCESO, MANANA.atTime(12, 0), MANANA.atTime(13, 0), 4.4, -74.3);
        mundo.agregarCita(5L, 2L, EstadoCita.CONFIRMADA, MANANA.atTime(9, 30), MANANA.atTime(10, 30), 4.4, -74.3);
        mundo.agregarCita(6L, 1L, EstadoCita.CONFIRMADA, MANANA.plusDays(1).atTime(9, 0),
                MANANA.plusDays(1).atTime(10, 0), 4.4, -74.3);

        List<CitaConCliente> citas = mundo.consultarCitas().consultar(CORREO_TECNICO, MANANA);

        assertEquals(List.of(1L, 4L, 2L), citas.stream().map(c -> c.cita().getId()).toList());
        assertEquals("Cliente1", citas.get(0).cliente().nombre());
        assertEquals("3001", citas.get(0).cliente().telefono());
    }

    // ---------------------------------------------------------------- ruta (SCRUM-15 y SCRUM-16)

    @Test
    void rutaVaciaCuandoNoHayCitasYNoLlamaAlPuerto() {
        RutaDelDia ruta = mundo.calcularRuta(0).calcular(CORREO_TECNICO, MANANA);

        assertTrue(ruta.paradas().isEmpty());
        assertEquals(0.0, ruta.totalKm());
        assertEquals(0.0, ruta.totalMinutosDeViaje());
        assertEquals(BASE, ruta.origen());
        assertEquals(AHORA, ruta.calculadaEn());
        assertTrue(mundo.destinosConsultados.isEmpty());
    }

    @Test
    void rutaConLasParadasEnOrdenDeHoraYTramosEncadenados() {
        // Se crean desordenadas y la más lejana de la base es la primera por hora: no se reordena por cercanía.
        confirmada(3L, MANANA.atTime(14, 0), MANANA.atTime(15, 0), PUNTO_C);
        confirmada(1L, MANANA.atTime(9, 0), MANANA.atTime(10, 0), PUNTO_A);
        confirmada(2L, MANANA.atTime(11, 0), MANANA.atTime(12, 0), PUNTO_B);
        mundo.distanciasPorDestino.put(PUNTO_A, new Distancia(10.0, 20.0));
        mundo.distanciasPorDestino.put(PUNTO_B, new Distancia(5.0, 10.0));
        mundo.distanciasPorDestino.put(PUNTO_C, new Distancia(7.5, 15.0));

        RutaDelDia ruta = mundo.calcularRuta(0).calcular(CORREO_TECNICO, MANANA);

        assertEquals(List.of(1L, 2L, 3L), ruta.paradas().stream().map(ParadaRuta::citaId).toList());
        assertEquals(List.of(1, 2, 3), ruta.paradas().stream().map(ParadaRuta::orden).toList());
        // Una llamada por tramo: base->A, A->B, B->C.
        assertEquals(List.of(BASE, PUNTO_A, PUNTO_B), mundo.origenesConsultados);
        assertEquals(List.of(List.of(PUNTO_A), List.of(PUNTO_B), List.of(PUNTO_C)), mundo.destinosConsultados);
        assertEquals(22.5, ruta.totalKm(), 1e-9);
        assertEquals(45.0, ruta.totalMinutosDeViaje(), 1e-9);
        assertEquals("Cliente2", ruta.paradas().get(1).cliente().nombre());
    }

    @Test
    void laPrimeraParadaMuestraLaHoraDeSalidaYSiempreAlcanzaATiempo() {
        confirmada(1L, MANANA.atTime(9, 0), MANANA.atTime(10, 0), PUNTO_A);
        mundo.distanciasPorDestino.put(PUNTO_A, new Distancia(10.0, 30.0));

        ParadaRuta primera = mundo.calcularRuta(0).calcular(CORREO_TECNICO, MANANA).paradas().get(0);

        assertEquals(MANANA.atTime(8, 30), primera.horaEstimadaLlegada());
        assertTrue(primera.alcanzaATiempo());
        assertEquals(10.0, primera.distanciaKm());
        assertEquals(30.0, primera.minutosDesdeAnterior());
    }

    @Test
    void alcanzaATiempoEsFalsoCuandoElViajeNoCabeEntreCitas() {
        confirmada(1L, MANANA.atTime(9, 0), MANANA.atTime(10, 0), PUNTO_A);
        confirmada(2L, MANANA.atTime(10, 20), MANANA.atTime(11, 0), PUNTO_B);
        confirmada(3L, MANANA.atTime(12, 0), MANANA.atTime(13, 0), PUNTO_C);
        mundo.distanciasPorDestino.put(PUNTO_A, new Distancia(1.0, 5.0));
        mundo.distanciasPorDestino.put(PUNTO_B, new Distancia(20.0, 30.0));   // llega 10:30 > 10:20
        mundo.distanciasPorDestino.put(PUNTO_C, new Distancia(5.0, 20.0));    // llega 11:20 <= 12:00

        List<ParadaRuta> paradas = mundo.calcularRuta(0).calcular(CORREO_TECNICO, MANANA).paradas();

        assertEquals(MANANA.atTime(10, 30), paradas.get(1).horaEstimadaLlegada());
        assertFalse(paradas.get(1).alcanzaATiempo());
        assertTrue(paradas.get(2).alcanzaATiempo());
    }

    @Test
    void citasConsecutivasEnLaMismaUbicacionAlcanzanConViajeCero() {
        confirmada(1L, MANANA.atTime(8, 0), MANANA.atTime(10, 0), PUNTO_A);
        confirmada(2L, MANANA.atTime(10, 0), MANANA.atTime(11, 0), PUNTO_A);
        mundo.distanciasPorDestino.put(PUNTO_A, new Distancia(0.0, 0.0));

        List<ParadaRuta> paradas = mundo.calcularRuta(0).calcular(CORREO_TECNICO, MANANA).paradas();

        assertEquals(MANANA.atTime(8, 0), paradas.get(0).horaEstimadaLlegada());   // sale de la base a las 8:00
        assertEquals(MANANA.atTime(10, 0), paradas.get(1).horaEstimadaLlegada());
        assertEquals(0.0, paradas.get(1).minutosDesdeAnterior());
        assertTrue(paradas.get(1).alcanzaATiempo());
    }

    @Test
    void ubicacionesDistintasConTiempoInsuficienteNoAlcanzan() {
        confirmada(1L, MANANA.atTime(8, 0), MANANA.atTime(10, 0), PUNTO_A);
        confirmada(2L, MANANA.atTime(10, 0), MANANA.atTime(11, 0), PUNTO_B);
        mundo.distanciasPorDestino.put(PUNTO_A, new Distancia(1.0, 5.0));
        mundo.distanciasPorDestino.put(PUNTO_B, new Distancia(15.0, 20.0));    // llega 10:20 > 10:00

        assertFalse(mundo.calcularRuta(0).calcular(CORREO_TECNICO, MANANA).paradas().get(1).alcanzaATiempo());
    }

    @Test
    void ubicacionesDistintasConTiempoSuficienteAlcanzan() {
        confirmada(1L, MANANA.atTime(8, 0), MANANA.atTime(10, 0), PUNTO_A);
        confirmada(2L, MANANA.atTime(10, 30), MANANA.atTime(11, 0), PUNTO_B);
        mundo.distanciasPorDestino.put(PUNTO_A, new Distancia(1.0, 5.0));
        mundo.distanciasPorDestino.put(PUNTO_B, new Distancia(15.0, 20.0));    // llega 10:20 <= 10:30

        assertTrue(mundo.calcularRuta(0).calcular(CORREO_TECNICO, MANANA).paradas().get(1).alcanzaATiempo());
    }

    // ---- tolerancia de retraso ----

    private ParadaRuta segundaParadaConViaje(double minutosDeViaje, int tolerancia) {
        confirmada(1L, MANANA.atTime(8, 0), MANANA.atTime(10, 0), PUNTO_A);
        confirmada(2L, MANANA.atTime(10, 0), MANANA.atTime(11, 0), PUNTO_B);
        mundo.distanciasPorDestino.put(PUNTO_A, new Distancia(1.0, 5.0));
        mundo.distanciasPorDestino.put(PUNTO_B, new Distancia(0.05, minutosDeViaje));
        return mundo.calcularRuta(0, tolerancia).calcular(CORREO_TECNICO, MANANA).paradas().get(1);
    }

    @Test
    void sinRetrasoAlcanzaYElRetrasoEsCero() {
        ParadaRuta p = segundaParadaConViaje(0.0, 5);

        assertTrue(p.alcanzaATiempo());
        assertEquals(0, p.retrasoEstimadoMinutos());
    }

    @Test
    void retrasoMenorQueLaToleranciaAlcanzaYSeInforma() {
        ParadaRuta p = segundaParadaConViaje(3.0, 5);

        assertTrue(p.alcanzaATiempo());
        assertEquals(3, p.retrasoEstimadoMinutos());
    }

    @Test
    void unosSegundosDeViajeEnCitasPegadasAlcanzanConLaToleranciaPorDefecto() {
        ParadaRuta p = segundaParadaConViaje(0.1, 5);   // 6 segundos

        assertTrue(p.alcanzaATiempo());
        assertEquals(1, p.retrasoEstimadoMinutos());
    }

    @Test
    void retrasoIgualALaToleranciaAlcanza() {
        ParadaRuta p = segundaParadaConViaje(5.0, 5);

        assertTrue(p.alcanzaATiempo());
        assertEquals(5, p.retrasoEstimadoMinutos());
    }

    @Test
    void retrasoMayorQueLaToleranciaNoAlcanza() {
        ParadaRuta p = segundaParadaConViaje(6.0, 5);

        assertFalse(p.alcanzaATiempo());
        assertEquals(6, p.retrasoEstimadoMinutos());
    }

    @Test
    void conToleranciaCeroLaReglaEsEstrictaComoAntes() {
        assertTrue(segundaParadaConViaje(0.0, 0).alcanzaATiempo());
    }

    @Test
    void conToleranciaCeroUnRetrasoDeSegundosYaNoAlcanza() {
        ParadaRuta tarde = segundaParadaConViaje(0.1, 0);

        assertFalse(tarde.alcanzaATiempo());
        assertEquals(1, tarde.retrasoEstimadoMinutos());
    }

    @Test
    void laPrimeraParadaNuncaTieneRetraso() {
        confirmada(1L, MANANA.atTime(9, 0), MANANA.atTime(10, 0), PUNTO_A);
        mundo.distanciasPorDestino.put(PUNTO_A, new Distancia(10.0, 30.0));

        ParadaRuta primera = mundo.calcularRuta(0, 0).calcular(CORREO_TECNICO, MANANA).paradas().get(0);

        assertTrue(primera.alcanzaATiempo());
        assertEquals(0, primera.retrasoEstimadoMinutos());
    }

    @Test
    void elMargenConfigurableSeSumaAlViajeAlDecidirSiAlcanza() {
        confirmada(1L, MANANA.atTime(9, 0), MANANA.atTime(10, 0), PUNTO_A);
        confirmada(2L, MANANA.atTime(10, 20), MANANA.atTime(11, 0), PUNTO_B);
        mundo.distanciasPorDestino.put(PUNTO_B, new Distancia(3.0, 10.0));    // llega 10:10

        assertTrue(mundo.calcularRuta(0).calcular(CORREO_TECNICO, MANANA).paradas().get(1).alcanzaATiempo());
        assertTrue(mundo.calcularRuta(10).calcular(CORREO_TECNICO, MANANA).paradas().get(1).alcanzaATiempo());
        assertFalse(mundo.calcularRuta(15).calcular(CORREO_TECNICO, MANANA).paradas().get(1).alcanzaATiempo());
    }

    @Test
    void tecnicoSinCoordenadasBaseRecibeMensajeClaro() {
        mundo.registrarTecnico(1L, 10L, CORREO_TECNICO, null, null);
        confirmada(1L, MANANA.atTime(9, 0), MANANA.atTime(10, 0), PUNTO_A);

        UbicacionBaseNoConfiguradaException error = assertThrows(UbicacionBaseNoConfiguradaException.class,
                () -> mundo.calcularRuta(0).calcular(CORREO_TECNICO, MANANA));

        assertEquals("Tu ubicación base no está configurada; pídele al administrador que la cargue",
                error.getMessage());
        assertTrue(mundo.destinosConsultados.isEmpty());
    }

    @Test
    void citaSinCoordenadasValidasEs409ConElIdDeLaCitaYNoLlamaAlPuerto() {
        confirmada(1L, MANANA.atTime(9, 0), MANANA.atTime(10, 0), PUNTO_A);
        mundo.agregarCita(42L, 1L, EstadoCita.CONFIRMADA, MANANA.atTime(11, 0), MANANA.atTime(12, 0), null, null);

        CitaSinUbicacionException sinDatos = assertThrows(CitaSinUbicacionException.class,
                () -> mundo.calcularRuta(0).calcular(CORREO_TECNICO, MANANA));
        assertTrue(sinDatos.getMessage().contains("42"));

        mundo.citas.removeIf(c -> c.getId() == 42L);
        mundo.agregarCita(43L, 1L, EstadoCita.CONFIRMADA, MANANA.atTime(11, 0), MANANA.atTime(12, 0), -74.3, 4.4);
        CitaSinUbicacionException invertida = assertThrows(CitaSinUbicacionException.class,
                () -> mundo.calcularRuta(0).calcular(CORREO_TECNICO, MANANA));
        assertTrue(invertida.getMessage().contains("43"));
        assertTrue(mundo.destinosConsultados.isEmpty());
    }

    @Test
    void laRutaSeRecalculaAlAgregarOCancelarUnaCitaPorqueNadaSeGuarda() {
        Cita primera = confirmada(1L, MANANA.atTime(9, 0), MANANA.atTime(10, 0), PUNTO_A);
        mundo.distanciasPorDestino.put(PUNTO_A, new Distancia(10.0, 20.0));
        mundo.distanciasPorDestino.put(PUNTO_B, new Distancia(5.0, 10.0));
        var useCase = mundo.calcularRuta(0);

        RutaDelDia antes = useCase.calcular(CORREO_TECNICO, MANANA);
        assertEquals(1, antes.paradas().size());
        assertEquals(10.0, antes.totalKm());

        confirmada(2L, MANANA.atTime(11, 0), MANANA.atTime(12, 0), PUNTO_B);
        RutaDelDia conNueva = useCase.calcular(CORREO_TECNICO, MANANA);
        assertEquals(2, conNueva.paradas().size());
        assertEquals(15.0, conNueva.totalKm());

        primera.setEstado(EstadoCita.CANCELADA);
        RutaDelDia trasCancelar = useCase.calcular(CORREO_TECNICO, MANANA);
        assertEquals(List.of(2L), trasCancelar.paradas().stream().map(ParadaRuta::citaId).toList());
        assertEquals(5.0, trasCancelar.totalKm());
        // El tramo ahora sale desde la base, porque la cita anterior ya no existe.
        assertEquals(BASE, mundo.origenesConsultados.get(mundo.origenesConsultados.size() - 1));
    }

    @Test
    void elUsuarioSinPerfilDeTecnicoNoPuedeVerRutaNiCitas() {
        assertThrows(PerfilTecnicoNoEncontradoException.class,
                () -> mundo.calcularRuta(0).calcular("nadie@correo.com", MANANA));
        assertThrows(PerfilTecnicoNoEncontradoException.class,
                () -> mundo.consultarCitas().consultar("nadie@correo.com", MANANA));
    }
}
