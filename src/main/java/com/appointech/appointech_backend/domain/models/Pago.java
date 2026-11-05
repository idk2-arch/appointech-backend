package com.appointech.appointech_backend.domain.models;

import com.appointech.appointech_backend.domain.exception.DatosPagoInvalidosException;
import com.appointech.appointech_backend.domain.exception.PagoInvalidoException;
import com.appointech.appointech_backend.domain.exception.TransaccionNoCoincideException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Pago de una cita. Guarda el id de la cita, no el objeto. Un pago PAGADO es definitivo: ninguna operación lo cambia.
 */
public class Pago {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    /** Tope de la columna NUMERIC(12,2). */
    private static final BigDecimal MONTO_MAXIMO = new BigDecimal("9999999999.99");

    private Long id;
    private Long citaId;
    private BigDecimal montoCotizado;
    private BigDecimal montoFinal;
    private MetodoPago metodo;
    private EstadoPago estado;
    private String referenciaExterna;
    private String idTransaccionExterna;
    private int intentosPago;
    private LocalDateTime pagadoEn;

    public Pago() {
    }

    public static Pago nuevo(Long citaId) {
        Pago pago = new Pago();
        pago.citaId = citaId;
        pago.estado = EstadoPago.PENDIENTE;
        return pago;
    }

    // ---- Reglas del ciclo de vida (sin framework) ----

    /** Guarda el valor estimado mostrado al cliente; queda congelado cuando ya hay costo final o el pago está hecho. */
    public void registrarCotizacion(BigDecimal monto) {
        if (montoFinal != null || estado == EstadoPago.PAGADO) {
            return;
        }
        this.montoCotizado = Dinero.redondear(monto);
    }

    public void registrarCostoFinal(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new DatosPagoInvalidosException("El costo final debe ser un valor mayor que cero");
        }
        if (monto.compareTo(MONTO_MAXIMO) > 0) {
            throw new DatosPagoInvalidosException("El costo final supera el máximo permitido");
        }
        exigirNoPagado("cambiar el costo final");
        if (hayPagoEnLineaEnCurso()) {
            throw new PagoInvalidoException(
                    "Hay un pago en línea en curso; espera su resultado antes de cambiar el costo final");
        }
        this.montoFinal = Dinero.redondear(monto);
    }

    /** Verifica que ya se pueda cobrar: hay costo final, el pago no está hecho y no hay un pago en línea en proceso. */
    public void validarPuedeCobrarse() {
        exigirNoPagado("cambiar el método de pago");
        if (montoFinal == null) {
            throw new PagoInvalidoException("El técnico aún no registró el costo final del servicio");
        }
        if (hayPagoEnLineaEnCurso()) {
            throw new PagoInvalidoException(
                    "Hay un pago en línea en proceso; verifica su resultado antes de cambiar de método.");
        }
    }

    public void usarEfectivo() {
        validarPuedeCobrarse();
        this.metodo = MetodoPago.EFECTIVO;
        this.estado = EstadoPago.PENDIENTE;
    }

    /** Referencia que tendrá el próximo intento en línea: nueva y única por pago. */
    public String proximaReferencia() {
        if (id == null) {
            throw new IllegalStateException("El pago debe estar guardado para generar su referencia");
        }
        return "APT-" + citaId + "-" + id + "-" + (intentosPago + 1);
    }

    /** Registra un intento en línea ya aceptado por la pasarela. */
    public void registrarIntentoEnLinea(String referencia) {
        validarPuedeCobrarse();
        this.metodo = MetodoPago.EN_LINEA;
        this.estado = EstadoPago.PENDIENTE;
        this.referenciaExterna = referencia;
        this.idTransaccionExterna = null;
        this.intentosPago++;
    }

    public void confirmarEfectivo(LocalDateTime ahora) {
        exigirNoPagado("confirmar el pago");
        if (metodo != MetodoPago.EFECTIVO) {
            throw new PagoInvalidoException("El cliente no eligió pagar en efectivo");
        }
        marcarPagado(ahora);
    }

    /**
     * Aplica el estado de una transacción consultada a la pasarela. Exige que la referencia y el monto sean los de
     * este pago; un pago PAGADO no cambia (idempotente).
     */
    public void aplicarResultado(ResultadoTransaccion resultado, LocalDateTime ahora) {
        if (estado == EstadoPago.PAGADO) {
            return;
        }
        exigirPagoEnLineaIniciado();
        if (!referenciaExterna.equals(resultado.referencia())
                || resultado.montoEnCentavos() != Dinero.aCentavos(montoFinal)) {
            throw new TransaccionNoCoincideException();
        }
        this.idTransaccionExterna = resultado.idTransaccion();
        switch (resultado.estado()) {
            case APROBADA -> marcarPagado(ahora);
            case RECHAZADA, ERROR, ANULADA -> this.estado = EstadoPago.FALLIDO;
            case PENDIENTE -> this.estado = EstadoPago.PENDIENTE;
        }
    }

    /** Exige un pago en línea ya iniciado (con referencia); si no, no hay nada que consultar a la pasarela. */
    public void exigirPagoEnLineaIniciado() {
        if (metodo != MetodoPago.EN_LINEA || referenciaExterna == null) {
            throw new PagoInvalidoException("No hay un pago en línea iniciado para esta cita");
        }
    }

    /** Situación del cobro: pagado, por cobrar (ya hay costo final) o sin costo final. */
    public EstadoCobro estadoCobro() {
        if (estado == EstadoPago.PAGADO) {
            return EstadoCobro.PAGADO;
        }
        return montoFinal == null ? EstadoCobro.SIN_COSTO_FINAL : EstadoCobro.PENDIENTE;
    }

    /** Diferencia absoluta entre el costo final y el cotizado, en %; null si falta alguno de los dos. */
    public BigDecimal porcentajeDiferencia() {
        if (!hayBaseDeComparacion()) {
            return null;
        }
        return diferenciaAbsoluta().multiply(CIEN).divide(montoCotizado, 2, RoundingMode.HALF_UP);
    }

    /** True cuando la diferencia supera estrictamente el umbral (%). Se compara sin redondear. */
    public boolean diferenciaSignificativa(BigDecimal umbralPorcentaje) {
        return hayBaseDeComparacion()
                && diferenciaAbsoluta().multiply(CIEN).compareTo(umbralPorcentaje.multiply(montoCotizado)) > 0;
    }

    private boolean hayBaseDeComparacion() {
        return montoCotizado != null && montoFinal != null && montoCotizado.signum() > 0;
    }

    private BigDecimal diferenciaAbsoluta() {
        return montoFinal.subtract(montoCotizado).abs();
    }

    private boolean hayPagoEnLineaEnCurso() {
        return metodo == MetodoPago.EN_LINEA && estado == EstadoPago.PENDIENTE && referenciaExterna != null;
    }

    private void marcarPagado(LocalDateTime ahora) {
        this.estado = EstadoPago.PAGADO;
        this.pagadoEn = ahora;
    }

    private void exigirNoPagado(String accion) {
        if (estado == EstadoPago.PAGADO) {
            throw new PagoInvalidoException("El pago ya fue realizado y no se puede " + accion);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCitaId() {
        return citaId;
    }

    public void setCitaId(Long citaId) {
        this.citaId = citaId;
    }

    public BigDecimal getMontoCotizado() {
        return montoCotizado;
    }

    public void setMontoCotizado(BigDecimal montoCotizado) {
        this.montoCotizado = montoCotizado;
    }

    public BigDecimal getMontoFinal() {
        return montoFinal;
    }

    public void setMontoFinal(BigDecimal montoFinal) {
        this.montoFinal = montoFinal;
    }

    public MetodoPago getMetodo() {
        return metodo;
    }

    public void setMetodo(MetodoPago metodo) {
        this.metodo = metodo;
    }

    public EstadoPago getEstado() {
        return estado;
    }

    public void setEstado(EstadoPago estado) {
        this.estado = estado;
    }

    public String getReferenciaExterna() {
        return referenciaExterna;
    }

    public void setReferenciaExterna(String referenciaExterna) {
        this.referenciaExterna = referenciaExterna;
    }

    public String getIdTransaccionExterna() {
        return idTransaccionExterna;
    }

    public void setIdTransaccionExterna(String idTransaccionExterna) {
        this.idTransaccionExterna = idTransaccionExterna;
    }

    public int getIntentosPago() {
        return intentosPago;
    }

    public void setIntentosPago(int intentosPago) {
        this.intentosPago = intentosPago;
    }

    public LocalDateTime getPagadoEn() {
        return pagadoEn;
    }

    public void setPagadoEn(LocalDateTime pagadoEn) {
        this.pagadoEn = pagadoEn;
    }
}
