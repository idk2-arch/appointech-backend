package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.RegistroLog;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Registro de logs que no ocupa una segunda conexión de la base por petición: {@code registrar} entrega el
 * registro a un hilo propio (una sola conexión a la vez) y la petición sigue sin esperar. El guardado sigue siendo
 * el de {@link RegistroLogAdapter} (transacción propia, sobrevive a un rollback de la petición).
 * Si la cola se llena, la política del ejecutor hace que el propio hilo de la petición lo guarde (nunca se pierde un log).
 */
@Primary
@Component
public class
RegistroLogAsincronoAdapter implements RegistroLogPort {

    private static final Logger log = LoggerFactory.getLogger(RegistroLogAsincronoAdapter.class);

    private final RegistroLogAdapter guardado;
    private final ExecutorService ejecutor;

    public RegistroLogAsincronoAdapter(RegistroLogAdapter guardado, @Qualifier("registroLogExecutor") ExecutorService ejecutor) {
        this.guardado = guardado;
        this.ejecutor = ejecutor;
    }

    @Override
    public void registrar(NivelLog nivel, TipoLog tipo, String origen, String usuario, String mensaje) {
        ejecutor.execute(() -> {
            try {
                guardado.registrar(nivel, tipo, origen, usuario, mensaje);
            } catch (RuntimeException ex) {
                // Un log que no se pudo guardar no debe afectar a nadie más; queda en stdout
                log.warn("No se pudo guardar el log {} de {}: {}", tipo, origen, ex.getMessage());
            }
        });
    }

    @Override
    public List<RegistroLog> listar(String nivel, String tipo, int limite) {
        return guardado.listar(nivel, tipo, limite);
    }

    @Override
    public int eliminarAnterioresA(LocalDateTime fechaLimite) {
        return guardado.eliminarAnterioresA(fechaLimite);
    }
}
