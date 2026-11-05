package com.appointech.appointech_backend.domain.models;

import com.appointech.appointech_backend.domain.models.exception.ComprobanteNoDisponibleException;

import java.time.LocalDateTime;
import java.util.Locale;

public class ComprobantePago {
    private Long id;
    private Pago pago;
    private LocalDateTime emitidoEn;

    public ComprobantePago() {
    }

    public static ComprobantePago emitir(Pago pago) {
        if (!pago.estaPagado()) {
            throw new ComprobanteNoDisponibleException("El comprobante solo se genera con un pago confirmado");
        }
        ComprobantePago comprobante = new ComprobantePago();
        comprobante.pago = pago;
        comprobante.emitidoEn = LocalDateTime.now();
        return comprobante;
    }

    public String getNumero() {
        return id == null ? null : "COMP-" + String.format(Locale.ROOT, "%06d", id);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Pago getPago() { return pago; }
    public void setPago(Pago pago) { this.pago = pago; }
    public LocalDateTime getEmitidoEn() { return emitidoEn; }
    public void setEmitidoEn(LocalDateTime emitidoEn) { this.emitidoEn = emitidoEn; }
}
