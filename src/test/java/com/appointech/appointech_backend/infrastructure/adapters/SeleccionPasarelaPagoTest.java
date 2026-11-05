package com.appointech.appointech_backend.infrastructure.adapters;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.appointech.appointech_backend.domain.ports.out.PasarelaPagoPort;
import com.appointech.appointech_backend.infrastructure.config.PagosConfig;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La pasarela es fail-closed: sin configuración o con "wompi" nunca se activa la simulada. */
class SeleccionPasarelaPagoTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(PagosConfig.class);

    @Test
    void sinLaPropiedadQuedaElAdaptadorDeReservaYNuncaLaSimulada() {
        contexto.run(ctx -> {
            assertEquals(1, ctx.getBeansOfType(PasarelaPagoPort.class).size());
            assertInstanceOf(PasarelaPagoWompiPendienteAdapter.class, ctx.getBean(PasarelaPagoPort.class));
        });
    }

    @Test
    void conWompiQuedaElAdaptadorDeReserva() {
        contexto.withPropertyValues("pagos.pasarela=wompi").run(ctx -> {
            assertEquals(1, ctx.getBeansOfType(PasarelaPagoPort.class).size());
            assertInstanceOf(PasarelaPagoWompiPendienteAdapter.class, ctx.getBean(PasarelaPagoPort.class));
        });
    }

    @Test
    void conSimuladaExplicitaSeActivaLaSimuladaYSeAvisaConUnWarn() {
        Logger logger = (Logger) LoggerFactory.getLogger(PasarelaPagoSimuladaAdapter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            contexto.withPropertyValues("pagos.pasarela=simulada").run(ctx -> {
                assertEquals(1, ctx.getBeansOfType(PasarelaPagoPort.class).size());
                assertInstanceOf(PasarelaPagoSimuladaAdapter.class, ctx.getBean(PasarelaPagoPort.class));
            });
        } finally {
            logger.detachAppender(appender);
        }
        assertTrue(appender.list.stream().anyMatch(e -> e.getLevel() == Level.WARN
                && e.getFormattedMessage().equals("Pasarela de pagos SIMULADA activa: no usar en producción")));
    }

    @Test
    void laSimuladaRespetaElSwitchDeAprobacion() {
        contexto.withPropertyValues("pagos.pasarela=simulada", "pagos.simulada.aprobar=false").run(ctx -> assertEquals(
                com.appointech.appointech_backend.domain.models.EstadoTransaccionPasarela.RECHAZADA,
                ctx.getBean(PasarelaPagoPort.class).consultarPorReferencia("APT-1-1-1", 100).estado()));
    }

    @Test
    void elValorSeNormalizaSinImportarMayusculasNiEspacios() {
        contexto.withPropertyValues("pagos.pasarela= WOMPI ")
                .run(ctx -> assertInstanceOf(PasarelaPagoWompiPendienteAdapter.class, ctx.getBean(PasarelaPagoPort.class)));
    }

    @Test
    void conUnValorDesconocidoLaAplicacionNoArrancaYElMensajeIndicaLosValoresValidos() {
        contexto.withPropertyValues("pagos.pasarela=otra").run(ctx -> {
            assertTrue(ctx.getStartupFailure() != null);
            assertTrue(rootMessage(ctx.getStartupFailure()).contains("'otra' no es válido: use 'simulada' o 'wompi'"));
        });
    }

    private static String rootMessage(Throwable error) {
        Throwable actual = error;
        while (actual.getCause() != null) {
            actual = actual.getCause();
        }
        return actual.getMessage();
    }
}
