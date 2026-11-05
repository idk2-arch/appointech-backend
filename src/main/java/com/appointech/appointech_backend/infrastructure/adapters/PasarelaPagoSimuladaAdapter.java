package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.EstadoTransaccionPasarela;
import com.appointech.appointech_backend.domain.models.InicioPago;
import com.appointech.appointech_backend.domain.models.ResultadoTransaccion;
import com.appointech.appointech_backend.domain.models.SolicitudPago;
import com.appointech.appointech_backend.domain.ports.out.PasarelaPagoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Pasarela simulada para desarrollo y pruebas: no llama a ningún servicio externo ni guarda estado. El id de la
 * transacción lleva la referencia y el monto (SIM:referencia:centavos), así se puede consultar sin memoria propia.
 * El resultado de toda consulta válida lo fija PASARELA_SIMULADA_APROBAR. No es un bean: lo crea PagosConfig solo si
 * PASARELA_PAGOS=simulada.
 */
public class PasarelaPagoSimuladaAdapter implements PasarelaPagoPort {

    private static final String PREFIJO = "SIM:";

    private static final Logger log = LoggerFactory.getLogger(PasarelaPagoSimuladaAdapter.class);

    private final boolean aprobar;

    public PasarelaPagoSimuladaAdapter(boolean aprobar) {
        this.aprobar = aprobar;
        log.warn("Pasarela de pagos SIMULADA activa: no usar en producción");
    }

    @Override
    public InicioPago iniciarPago(SolicitudPago solicitud) {
        String idTransaccion = PREFIJO + solicitud.referencia() + ":" + solicitud.montoEnCentavos();
        String urlPago = UriComponentsBuilder.fromUriString(solicitud.urlRetorno())
                .queryParam("id", idTransaccion)
                .queryParam("referencia", solicitud.referencia())
                .encode()
                .build()
                .toUriString();
        return new InicioPago(solicitud.referencia(), urlPago);
    }

    @Override
    public ResultadoTransaccion consultarTransaccion(String idTransaccion) {
        String[] partes = idTransaccion != null && idTransaccion.startsWith(PREFIJO)
                ? idTransaccion.substring(PREFIJO.length()).split(":")
                : new String[0];
        if (partes.length != 2) {
            return desconocida(idTransaccion);
        }
        try {
            return new ResultadoTransaccion(idTransaccion, partes[0], Long.parseLong(partes[1]),
                    aprobar ? EstadoTransaccionPasarela.APROBADA : EstadoTransaccionPasarela.RECHAZADA);
        } catch (NumberFormatException ex) {
            return desconocida(idTransaccion);
        }
    }

    @Override
    public ResultadoTransaccion consultarPorReferencia(String referencia, long montoEnCentavos) {
        return new ResultadoTransaccion(PREFIJO + referencia + ":" + montoEnCentavos, referencia, montoEnCentavos,
                aprobar ? EstadoTransaccionPasarela.APROBADA : EstadoTransaccionPasarela.RECHAZADA);
    }

    /** Un id que esta pasarela no generó: sin referencia ni monto, para que ningún pago coincida con él. */
    private ResultadoTransaccion desconocida(String idTransaccion) {
        return new ResultadoTransaccion(idTransaccion, null, 0, EstadoTransaccionPasarela.ERROR);
    }
}
