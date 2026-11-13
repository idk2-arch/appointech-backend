package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.RegistroLog;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.infrastructure.config.RegistroLogConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistroLogAsincronoAdapterTest {

    private final RegistroLogAdapter guardado = mock(RegistroLogAdapter.class);
    private final ExecutorService ejecutor = new RegistroLogConfig().registroLogExecutor(10);
    private final RegistroLogAsincronoAdapter adapter = new RegistroLogAsincronoAdapter(guardado, ejecutor);

    @AfterEach
    void cerrar() {
        ejecutor.close();
    }

    @Test
    void registrarGuardaEnOtroHiloSinBloquearAlQueLlama() throws Exception {
        CountDownLatch guardadoYa = new CountDownLatch(1);
        AtomicReference<String> hilo = new AtomicReference<>();
        doAnswer(invocacion -> {
            hilo.set(Thread.currentThread().getName());
            guardadoYa.countDown();
            return null;
        }).when(guardado).registrar(NivelLog.INFO, TipoLog.AUDITORIA, "Origen", "ana@mail.com", "mensaje");

        adapter.registrar(NivelLog.INFO, TipoLog.AUDITORIA, "Origen", "ana@mail.com", "mensaje");

        assertTrue(guardadoYa.await(5, TimeUnit.SECONDS));
        assertEquals("registro-log", hilo.get());
        assertNotEquals(Thread.currentThread().getName(), hilo.get());
    }

    @Test
    void unFalloAlGuardarNoLlegaAQuienRegistraNiDetieneElSiguiente() throws Exception {
        CountDownLatch segundo = new CountDownLatch(1);
        doThrow(new IllegalStateException("base caida")).when(guardado)
                .registrar(NivelLog.ERROR, TipoLog.SISTEMA, "A", "u", "falla");
        doAnswer(invocacion -> {
            segundo.countDown();
            return null;
        }).when(guardado).registrar(NivelLog.INFO, TipoLog.SISTEMA, "B", "u", "ok");

        adapter.registrar(NivelLog.ERROR, TipoLog.SISTEMA, "A", "u", "falla");
        adapter.registrar(NivelLog.INFO, TipoLog.SISTEMA, "B", "u", "ok");

        assertTrue(segundo.await(5, TimeUnit.SECONDS));
    }

    @Test
    void alCerrarElEjecutorSeVaciaLoPendiente() {
        adapter.registrar(NivelLog.INFO, TipoLog.AUDITORIA, "O", "u", "m1");
        adapter.registrar(NivelLog.INFO, TipoLog.AUDITORIA, "O", "u", "m2");

        ejecutor.close();

        verify(guardado).registrar(NivelLog.INFO, TipoLog.AUDITORIA, "O", "u", "m1");
        verify(guardado).registrar(NivelLog.INFO, TipoLog.AUDITORIA, "O", "u", "m2");
    }

    @Test
    void listarYEliminarDelegan() {
        List<RegistroLog> logs = List.of();
        LocalDateTime limite = LocalDateTime.of(2026, 1, 1, 0, 0);
        when(guardado.listar("INFO", null, 5)).thenReturn(logs);
        when(guardado.eliminarAnterioresA(limite)).thenReturn(3);

        assertEquals(logs, adapter.listar("INFO", null, 5));
        assertEquals(3, adapter.eliminarAnterioresA(limite));
        verify(guardado, org.mockito.Mockito.never()).registrar(any(), any(), anyString(), anyString(), anyString());
    }
}
