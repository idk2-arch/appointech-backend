package com.appointech.appointech_backend.domain.ports.out;

import com.appointech.appointech_backend.domain.models.ResultadoPasarelaPago;

import java.math.BigDecimal;

public interface PasarelaPagoPort {
    ResultadoPasarelaPago procesarPago(Long citaId, BigDecimal monto, String correoCliente);
}
