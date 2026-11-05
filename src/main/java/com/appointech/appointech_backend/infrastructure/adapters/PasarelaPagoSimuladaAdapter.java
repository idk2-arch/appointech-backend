package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.ResultadoPasarelaPago;
import com.appointech.appointech_backend.domain.ports.out.PasarelaPagoPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PasarelaPagoSimuladaAdapter implements PasarelaPagoPort {

    private final boolean aprobarPagos;

    public PasarelaPagoSimuladaAdapter(@Value("${appointech.pagos.pasarela-simulada.aprobar:true}") boolean aprobarPagos) {
        this.aprobarPagos = aprobarPagos;
    }

    @Override
    public ResultadoPasarelaPago procesarPago(Long citaId, BigDecimal monto, String correoCliente) {
        String referencia = "SIM-" + citaId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new ResultadoPasarelaPago(aprobarPagos, referencia);
    }
}
