package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.ConfiguracionPagos;
import com.appointech.appointech_backend.domain.models.ResultadoMetodoPago;
import com.appointech.appointech_backend.domain.ports.in.ConfirmarPagoEfectivoUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarComprobanteUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarPagoUseCase;
import com.appointech.appointech_backend.domain.ports.in.ElegirMetodoPagoUseCase;
import com.appointech.appointech_backend.domain.ports.in.RegistrarCostoFinalUseCase;
import com.appointech.appointech_backend.domain.ports.in.SincronizarPagoUseCase;
import com.appointech.appointech_backend.infrastructure.dto.ComprobanteResponse;
import com.appointech.appointech_backend.infrastructure.dto.CostoFinalRequest;
import com.appointech.appointech_backend.infrastructure.dto.MetodoPagoRequest;
import com.appointech.appointech_backend.infrastructure.dto.PagoResponse;
import com.appointech.appointech_backend.infrastructure.dto.SincronizarPagoRequest;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pago de una cita: costo final, método, confirmación en efectivo, sincronización con la pasarela y comprobante. El
 * rol solo filtra la entrada; la pertenencia a la cita la decide cada caso de uso.
 */
@RestController
@RequestMapping("/api/citas/{citaId}")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Datos inválidos (por ejemplo, un monto o método no válido)"),
        @ApiResponse(responseCode = "403", description = "Sin permiso: el rol no corresponde o la cita no es suya"),
        @ApiResponse(responseCode = "404", description = "La cita o su pago no existen"),
        @ApiResponse(responseCode = "409", description = "El estado del pago no permite la operación")
})
public class PagoController {

    private final RegistrarCostoFinalUseCase registrarCostoFinal;
    private final ElegirMetodoPagoUseCase elegirMetodoPago;
    private final ConfirmarPagoEfectivoUseCase confirmarPagoEfectivo;
    private final SincronizarPagoUseCase sincronizarPago;
    private final ConsultarPagoUseCase consultarPago;
    private final ConsultarComprobanteUseCase consultarComprobante;
    private final ConfiguracionPagos configuracion;

    public PagoController(RegistrarCostoFinalUseCase registrarCostoFinal, ElegirMetodoPagoUseCase elegirMetodoPago,
                          ConfirmarPagoEfectivoUseCase confirmarPagoEfectivo, SincronizarPagoUseCase sincronizarPago,
                          ConsultarPagoUseCase consultarPago, ConsultarComprobanteUseCase consultarComprobante,
                          ConfiguracionPagos configuracion) {
        this.registrarCostoFinal = registrarCostoFinal;
        this.elegirMetodoPago = elegirMetodoPago;
        this.confirmarPagoEfectivo = confirmarPagoEfectivo;
        this.sincronizarPago = sincronizarPago;
        this.consultarPago = consultarPago;
        this.consultarComprobante = consultarComprobante;
        this.configuracion = configuracion;
    }

    @PatchMapping("/costo-final")
    @PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<PagoResponse> registrarCostoFinal(@PathVariable Long citaId,
                                                            @Valid @RequestBody CostoFinalRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(PagoResponseMapper.aRespuesta(
                registrarCostoFinal.registrar(authentication.getName(), citaId, request.monto()), configuracion));
    }

    @ApiResponse(responseCode = "503", description = "El pago en línea no está disponible; el efectivo sigue disponible")
    @PatchMapping("/pago/metodo")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PagoResponse> elegirMetodo(@PathVariable Long citaId,
                                                     @Valid @RequestBody MetodoPagoRequest request,
                                                     Authentication authentication) {
        ResultadoMetodoPago resultado = elegirMetodoPago.elegir(authentication.getName(), citaId, request.metodo());
        return ResponseEntity.ok(PagoResponseMapper.aRespuesta(resultado.pago(), resultado.urlPago(), configuracion));
    }

    @PatchMapping("/pago/confirmar-efectivo")
    @PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<PagoResponse> confirmarEfectivo(@PathVariable Long citaId, Authentication authentication) {
        return ResponseEntity.ok(PagoResponseMapper.aRespuesta(
                confirmarPagoEfectivo.confirmar(authentication.getName(), citaId), configuracion));
    }

    /** El cuerpo es opcional: sin idTransaccion se consulta a la pasarela por la referencia guardada en el pago. */
    @ApiResponse(responseCode = "409",
            description = "El pago no es en línea o aún no tiene referencia, o su estado no permite la operación")
    @ApiResponse(responseCode = "422", description = "La transacción de la pasarela no corresponde a este pago")
    @ApiResponse(responseCode = "503", description = "La pasarela no está disponible; el pago queda sin cambios")
    @PostMapping("/pago/sincronizar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO', 'ADMINISTRADOR')")
    public ResponseEntity<PagoResponse> sincronizar(@PathVariable Long citaId,
                                                    @Valid @RequestBody(required = false) SincronizarPagoRequest request,
                                                    Authentication authentication) {
        String idTransaccion = request == null ? null : request.idTransaccion();
        return ResponseEntity.ok(PagoResponseMapper.aRespuesta(
                sincronizarPago.sincronizar(authentication.getName(), citaId, idTransaccion), configuracion));
    }

    @GetMapping("/pago")
    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO', 'ADMINISTRADOR')")
    public ResponseEntity<PagoResponse> obtenerPago(@PathVariable Long citaId, Authentication authentication) {
        return ResponseEntity.ok(PagoResponseMapper.aRespuesta(
                consultarPago.consultar(authentication.getName(), citaId), configuracion));
    }

    /** Solo consulta: el comprobante se genera solo al quedar PAGADO el pago. */
    @GetMapping("/comprobante")
    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO', 'ADMINISTRADOR')")
    public ResponseEntity<ComprobanteResponse> obtenerComprobante(@PathVariable Long citaId,
                                                                  Authentication authentication) {
        return ResponseEntity.ok(PagoResponseMapper.aRespuesta(
                consultarComprobante.consultar(authentication.getName(), citaId)));
    }
}
