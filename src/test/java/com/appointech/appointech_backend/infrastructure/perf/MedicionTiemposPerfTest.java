package com.appointech.appointech_backend.infrastructure.perf;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.StandardEnvironment;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicionTiemposPerfTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(MedidorTiemposInterceptor.class);
    private ListAppender<ILoggingEvent> registros;

    @BeforeEach
    void capturarLogs() {
        registros = new ListAppender<>();
        registros.start();
        logger.addAppender(registros);
        logger.setLevel(Level.WARN);
    }

    @AfterEach
    void liberarLogs() {
        logger.detachAppender(registros);
    }

    /** Clase de prueba con una operación lenta y otra rápida. */
    static class ServicioDePrueba {
        String lenta() throws InterruptedException {
            Thread.sleep(40);
            return "ok";
        }

        String rapida() {
            return "ok";
        }

        void falla() {
            throw new IllegalStateException("boom");
        }
    }

    private ServicioDePrueba proxy(long umbralMs) {
        ProxyFactory fabrica = new ProxyFactory(new ServicioDePrueba());
        fabrica.setProxyTargetClass(true);
        fabrica.addAdvice(new MedidorTiemposInterceptor(umbralMs));
        return (ServicioDePrueba) fabrica.getProxy();
    }

    @Test
    void registraLentoCuandoSuperaElUmbral() throws Exception {
        assertEquals("ok", proxy(10).lenta());

        assertEquals(1, registros.list.size());
        String mensaje = registros.list.get(0).getFormattedMessage();
        assertTrue(mensaje.startsWith("[LENTO] ServicioDePrueba.lenta"), mensaje);
    }

    @Test
    void noRegistraNadaBajoElUmbral() {
        assertEquals("ok", proxy(60_000).rapida());

        assertTrue(registros.list.isEmpty());
    }

    @Test
    void propagaLaExcepcionDelMetodoMedido() {
        assertThrows(IllegalStateException.class, () -> proxy(60_000).falla());
    }

    @Test
    void elPuntoDeCorteSoloAlcanzaControladores() throws Exception {
        PuntoCorteCapasAplicacion corte = new PuntoCorteCapasAplicacion();
        Method toString = Object.class.getMethod("toString");
        Method propio = ServicioDePrueba.class.getDeclaredMethod("rapida");

        assertFalse(corte.getClassFilter().matches(ServicioDePrueba.class));
        assertFalse(corte.matches(toString, ServicioDePrueba.class));
        assertTrue(corte.matches(propio, ServicioDePrueba.class));
        assertFalse(corte.getClassFilter().matches(
                com.appointech.appointech_backend.application.usecases.ListarEspecialidadesUseCaseImpl.class));
        assertFalse(corte.getClassFilter().matches(
                com.appointech.appointech_backend.infrastructure.adapters.CitaRepositoryAdapter.class));
        assertTrue(corte.getClassFilter().matches(
                com.appointech.appointech_backend.infrastructure.controllers.DisponibilidadController.class));
    }

    @Test
    void registraUnaSolaVezPorPeticion() throws Exception {
        var atributos = new org.springframework.web.context.request.ServletRequestAttributes(
                new org.springframework.mock.web.MockHttpServletRequest());
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(atributos);
        try {
            ServicioDePrueba servicio = proxy(0);
            servicio.lenta();
            servicio.lenta();
            assertEquals(1, registros.list.size());
        } finally {
            org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void laConfiguracionSoloSeActivaConElPerfilPerf() {
        assertTrue(MedicionTiemposPerfConfig.class.isAnnotationPresent(Profile.class));
        assertEquals("perf", MedicionTiemposPerfConfig.class.getAnnotation(Profile.class).value()[0]);

        Advisor advisor = new MedicionTiemposPerfConfig().medicionTiemposAdvisor(250);
        assertTrue(advisor.getAdvice() instanceof MedidorTiemposInterceptor);
    }

    @Test
    void sinPerfilPerfElAdvisorNoSeRegistra() {
        try (AnnotationConfigApplicationContext contexto = new AnnotationConfigApplicationContext()) {
            contexto.setEnvironment(new StandardEnvironment());
            contexto.register(MedicionTiemposPerfConfig.class);
            contexto.refresh();
            assertTrue(contexto.getBeansOfType(Advisor.class).isEmpty());
        }
    }
}
