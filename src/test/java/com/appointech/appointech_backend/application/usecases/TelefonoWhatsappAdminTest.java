package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.TelefonoWhatsappInvalidoException;
import com.appointech.appointech_backend.domain.models.CoberturaConfigurada;
import com.appointech.appointech_backend.domain.models.ConfiguracionFranjas;
import com.appointech.appointech_backend.domain.models.ContactoAdminConfig;
import com.appointech.appointech_backend.domain.models.Distancia;
import com.appointech.appointech_backend.domain.models.MotivoSinFranjas;
import com.appointech.appointech_backend.domain.models.ResultadoFranjas;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.FiltrarTecnicosPorEspecialidadUseCase;
import com.appointech.appointech_backend.domain.ports.out.BloqueIndisponibilidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.CalculadoraDistanciaPort;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.ConfiguracionFranjasRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelefonoWhatsappAdminTest {

    private static final LocalDate MANANA = LocalDate.now().plusDays(1);

    private ConfiguracionFranjasRepositoryPort configuracionPort;
    private ConfiguracionFranjas configuracion;
    private CoberturaUseCasesImpl cobertura;

    @BeforeEach
    void preparar() {
        configuracion = new ConfiguracionFranjas(1L, 120, 15, LocalTime.of(8, 0), LocalTime.of(18, 0));
        configuracionPort = mock(ConfiguracionFranjasRepositoryPort.class);
        when(configuracionPort.obtenerActual()).thenReturn(Optional.of(configuracion));
        when(configuracionPort.guardar(any(ConfiguracionFranjas.class))).thenAnswer(i -> i.getArgument(0));
        cobertura = new CoberturaUseCasesImpl(configuracionPort);
    }

    // ---------- guardar y validar ----------

    @Test
    void telefonoValidoSeGuardaYSeDevuelve() {
        CoberturaConfigurada r = cobertura.actualizar(15, "+57 300-123 4567");

        assertEquals("+57 300-123 4567", r.telefonoWhatsappAdmin());
        assertEquals("+57 300-123 4567", configuracion.getTelefonoWhatsappAdmin());
    }

    @Test
    void aceptaElLimiteDeDiezYQuinceDigitos() {
        assertEquals("3001234567", cobertura.actualizar(15, "3001234567").telefonoWhatsappAdmin());
        assertEquals("+573001234567890", cobertura.actualizar(15, "+573001234567890").telefonoWhatsappAdmin());
    }

    @Test
    void telefonoInvalidoLanza400ConMensajeClaroYNoGuarda() {
        String[] invalidos = {"123456789", "1234567890123456", "abc3001234567", "300.123.4567", "300 123 (4567)",
                "30+0123456789", "++573001234567", "+", "- 3001234567"};
        for (String invalido : invalidos) {
            TelefonoWhatsappInvalidoException ex = assertThrows(TelefonoWhatsappInvalidoException.class,
                    () -> cobertura.actualizar(15, invalido), "debería rechazar: " + invalido);
            assertEquals("El teléfono de WhatsApp debe tener entre 10 y 15 dígitos y solo puede incluir dígitos, "
                    + "espacios, guiones y un signo + al inicio", ex.getMessage());
        }
        verify(configuracionPort, never()).guardar(any());
    }

    @Test
    void telefonoVacioBorraElGuardado() {
        configuracion.setTelefonoWhatsappAdmin("3001234567");

        assertNull(cobertura.actualizar(15, "   ").telefonoWhatsappAdmin());
        assertNull(configuracion.getTelefonoWhatsappAdmin());
    }

    @Test
    void telefonoNuloNoModificaElGuardado() {
        configuracion.setTelefonoWhatsappAdmin("3001234567");

        assertEquals("3001234567", cobertura.actualizar(20, null).telefonoWhatsappAdmin());
        assertEquals(20, configuracion.getRadioMaximoKm());
    }

    @Test
    void elValorInicialEstaVacio() {
        assertNull(cobertura.obtener().telefonoWhatsappAdmin());
    }

    // ---------- respaldo por variable de entorno ----------

    @Test
    void laBaseDeDatosTienePrioridadSobreLaVariableDeEntorno() {
        assertEquals("3001234567", new ContactoAdminConfig("3109998877").resolver("3001234567"));
    }

    @Test
    void sinTelefonoEnBaseDeDatosSeUsaElRespaldo() {
        assertEquals("3109998877", new ContactoAdminConfig(" 3109998877 ").resolver(null));
        assertEquals("3109998877", new ContactoAdminConfig("3109998877").resolver("  "));
    }

    @Test
    void sinNingunoConfiguradoEsNulo() {
        assertNull(new ContactoAdminConfig(null).resolver(null));
        assertNull(new ContactoAdminConfig("").resolver(""));
        assertNull(new ContactoAdminConfig("   ").resolver(null));
    }

    // ---------- en la respuesta de franjas ----------

    private CalcularFranjasDisponiblesUseCaseImpl calcular(ContactoAdminConfig contacto, boolean fuera) {
        FiltrarTecnicosPorEspecialidadUseCase filtrar = mock(FiltrarTecnicosPorEspecialidadUseCase.class);
        Usuario u = new Usuario();
        u.setId(100L);
        u.setActivo(true);
        u.setTelefono("3000000000");     // el de un técnico jamás debe aparecer
        Tecnico t = new Tecnico();
        t.setId(1L);
        t.setUsuario(u);
        t.setLatitudBase(4.40);
        t.setLongitudBase(-74.30);
        when(filtrar.filtrar(5L)).thenReturn(List.of(t));
        CalculadoraDistanciaPort distancia = mock(CalculadoraDistanciaPort.class);
        when(distancia.calcular(any(), any())).thenReturn(List.of(new Distancia(fuera ? 90.0 : 3.0, 60.0)));
        return new CalcularFranjasDisponiblesUseCaseImpl(filtrar, mock(CitaRepositoryPort.class),
                mock(BloqueIndisponibilidadRepositoryPort.class), configuracionPort, distancia, contacto);
    }

    @Test
    void fueraDeCoberturaDevuelveElTelefonoDeLaBaseDeDatos() {
        configuracion.setTelefonoWhatsappAdmin("+57 300 123 4567");

        ResultadoFranjas r = calcular(new ContactoAdminConfig("3109998877"), true)
                .calcularConMotivo(5L, MANANA, 4.3365, -74.3638);

        assertEquals(MotivoSinFranjas.FUERA_DE_COBERTURA, r.motivo());
        assertEquals("+57 300 123 4567", r.telefonoContactoAdmin());
    }

    @Test
    void fueraDeCoberturaUsaElRespaldoSiLaBaseDeDatosEstaVacia() {
        ResultadoFranjas r = calcular(new ContactoAdminConfig("3109998877"), true)
                .calcularConMotivo(5L, MANANA, 4.3365, -74.3638);

        assertEquals("3109998877", r.telefonoContactoAdmin());
    }

    @Test
    void fueraDeCoberturaSinNingunTelefonoDevuelveNulo() {
        ResultadoFranjas r = calcular(new ContactoAdminConfig(null), true)
                .calcularConMotivo(5L, MANANA, 4.3365, -74.3638);

        assertEquals(MotivoSinFranjas.FUERA_DE_COBERTURA, r.motivo());
        assertNull(r.telefonoContactoAdmin());
    }

    @Test
    void conCoberturaNoSeExponeNingunTelefono() {
        configuracion.setTelefonoWhatsappAdmin("3001234567");

        ResultadoFranjas r = calcular(new ContactoAdminConfig("3109998877"), false)
                .calcularConMotivo(5L, MANANA, 4.3365, -74.3638);

        assertNull(r.motivo());
        assertNull(r.telefonoContactoAdmin());
    }

    @Test
    void nuncaSeExponeElTelefonoDeUnTecnico() {
        ResultadoFranjas r = calcular(new ContactoAdminConfig(null), true)
                .calcularConMotivo(5L, MANANA, 4.3365, -74.3638);

        assertNull(r.telefonoContactoAdmin());   // el técnico tiene 3000000000 y no se filtra
    }
}
