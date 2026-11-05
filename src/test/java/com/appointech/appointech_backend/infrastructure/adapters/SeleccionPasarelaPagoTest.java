package com.appointech.appointech_backend.infrastructure.adapters;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.appointech.appointech_backend.domain.ports.out.PasarelaPagoPort;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La pasarela es fail-closed: sin configuración o con "wompi" nunca se activa la simulada. */
class SeleccionPasarelaPagoTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(PasarelaPagoSimuladaAdapter.class, PasarelaPagoWompiPendienteAdapter.class);

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
    void conUnValorDesconocidoNoSeActivaNingunaPasarela() {
        contexto.withPropertyValues("pagos.pasarela=otra")
                .run(ctx -> assertEquals(0, ctx.getBeansOfType(PasarelaPagoPort.class).size()));
    }
}
