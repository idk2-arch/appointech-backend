package com.appointech.appointech_backend.domain.models;

import com.appointech.appointech_backend.domain.models.exception.CostoFinalNoPermitidoException;
import com.appointech.appointech_backend.domain.models.exception.DatosPagoInvalidosException;
import com.appointech.appointech_backend.domain.models.exception.PagoNoPermitidoException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class Pago {
    private Long id;
    private Cita cita;
    private BigDecimal montoCotizado;
    private BigDecimal montoFinal;
    private MetodoPago metodo;
    private EstadoPago estado;
    private String referenciaExterna;
    private LocalDateTime pagadoEn;

    public Pago() {
    }

    public static Pago nuevo(Cita cita) {
        Pago pago = new Pago();
        pago.cita = cita;
        pago.estado = EstadoPago.PENDIENTE;
        return pago;
    }

    public boolean registrarCotizacion(BigDecimal monto) {
        if (montoCotizado != null) {
            return false;
        }
        this.montoCotizado = monto;
        return true;
    }

    public void registrarCostoFinal(BigDecimal monto) {
        if (cita.getEstado() != EstadoCita.FINALIZADA) {
            throw new CostoFinalNoPermitidoException(cita.getEstado());
        }
        if (monto == null || monto.signum() <= 0) {
            throw new DatosPagoInvalidosException("El costo final debe ser mayor que cero");
        }
        if (estado == EstadoPago.PAGADO) {
            throw new PagoNoPermitidoException("El pago ya fue confirmado; no se puede modificar el costo final");
        }
        this.montoFinal = monto.setScale(2, RoundingMode.HALF_UP);
    }

    public void seleccionarMetodo(MetodoPago nuevoMetodo) {
        if (nuevoMetodo == null) {
            throw new DatosPagoInvalidosException("Debes indicar el método de pago");
        }
        if (montoFinal == null) {
            throw new PagoNoPermitidoException("El pago exige que el técnico registre primero el costo final");
        }
        if (estado == EstadoPago.PAGADO) {
            throw new PagoNoPermitidoException("El pago de esta cita ya fue confirmado");
        }
        this.metodo = nuevoMetodo;
        this.estado = EstadoPago.PENDIENTE;
        this.referenciaExterna = null;
        this.pagadoEn = null;
    }

    public void confirmarEfectivo() {
        if (metodo != MetodoPago.EFECTIVO || estado != EstadoPago.PENDIENTE) {
            throw new PagoNoPermitidoException("No hay un pago en efectivo pendiente de confirmar");
        }
        this.estado = EstadoPago.PAGADO;
        this.pagadoEn = LocalDateTime.now();
    }

    public void registrarResultadoEnLinea(boolean aprobado, String referencia) {
        if (metodo != MetodoPago.EN_LINEA || estado != EstadoPago.PENDIENTE) {
            throw new PagoNoPermitidoException("No hay un pago en línea pendiente de resultado");
        }
        this.referenciaExterna = referencia;
        if (aprobado) {
            this.estado = EstadoPago.PAGADO;
            this.pagadoEn = LocalDateTime.now();
        } else {
            this.estado = EstadoPago.FALLIDO;
        }
    }

    public boolean estaPagado() {
        return estado == EstadoPago.PAGADO;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cita getCita() { return cita; }
    public void setCita(Cita cita) { this.cita = cita; }
    public BigDecimal getMontoCotizado() { return montoCotizado; }
    public void setMontoCotizado(BigDecimal montoCotizado) { this.montoCotizado = montoCotizado; }
    public BigDecimal getMontoFinal() { return montoFinal; }
    public void setMontoFinal(BigDecimal montoFinal) { this.montoFinal = montoFinal; }
    public MetodoPago getMetodo() { return metodo; }
    public void setMetodo(MetodoPago metodo) { this.metodo = metodo; }
    public EstadoPago getEstado() { return estado; }
    public void setEstado(EstadoPago estado) { this.estado = estado; }
    public String getReferenciaExterna() { return referenciaExterna; }
    public void setReferenciaExterna(String referenciaExterna) { this.referenciaExterna = referenciaExterna; }
    public LocalDateTime getPagadoEn() { return pagadoEn; }
    public void setPagadoEn(LocalDateTime pagadoEn) { this.pagadoEn = pagadoEn; }
}
