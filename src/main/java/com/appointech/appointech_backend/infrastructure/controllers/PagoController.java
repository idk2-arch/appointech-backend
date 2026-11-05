package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.ComprobantePago;
import com.appointech.appointech_backend.domain.models.Cotizacion;
import com.appointech.appointech_backend.domain.models.Pago;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.in.ConsultarCotizacionUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarPagoUseCase;
import com.appointech.appointech_backend.domain.ports.in.GenerarComprobanteUseCase;
import com.appointech.appointech_backend.domain.ports.in.RegistrarCostoFinalUseCase;
import com.appointech.appointech_backend.domain.ports.in.SeleccionarMetodoPagoUseCase;
import com.appointech.appointech_backend.infrastructure.dto.ComprobanteResponse;
import com.appointech.appointech_backend.infrastructure.dto.CotizacionResponse;
import com.appointech.appointech_backend.infrastructure.dto.PagoResponse;
import com.appointech.appointech_backend.infrastructure.dto.RegistrarCostoFinalRequest;
import com.appointech.appointech_backend.infrastructure.dto.SeleccionarMetodoPagoRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/citas/{citaId}")
public class PagoController {

    private static final String NOTA_COTIZACION =
            "Valor estimado. El costo final lo registra el técnico al finalizar el servicio.";

    private final ConsultarCotizacionUseCase consultarCotizacionUseCase;
    private final RegistrarCostoFinalUseCase registrarCostoFinalUseCase;
    private final ConsultarPagoUseCase consultarPagoUseCase;
    private final SeleccionarMetodoPagoUseCase seleccionarMetodoPagoUseCase;
    private final GenerarComprobanteUseCase generarComprobanteUseCase;

    public PagoController(ConsultarCotizacionUseCase consultarCotizacionUseCase,
                          RegistrarCostoFinalUseCase registrarCostoFinalUseCase,
                          ConsultarPagoUseCase consultarPagoUseCase,
                          SeleccionarMetodoPagoUseCase seleccionarMetodoPagoUseCase,
                          GenerarComprobanteUseCase generarComprobanteUseCase) {
        this.consultarCotizacionUseCase = consultarCotizacionUseCase;
        this.registrarCostoFinalUseCase = registrarCostoFinalUseCase;
        this.consultarPagoUseCase = consultarPagoUseCase;
        this.seleccionarMetodoPagoUseCase = seleccionarMetodoPagoUseCase;
        this.generarComprobanteUseCase = generarComprobanteUseCase;
    }

    @GetMapping("/cotizacion")
    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO', 'ADMINISTRADOR')")
    public ResponseEntity<CotizacionResponse> cotizacion(@PathVariable Long citaId, Authentication authentication) {
        Cotizacion cotizacion = consultarCotizacionUseCase.consultar(citaId, authentication.getName());
        return ResponseEntity.ok(new CotizacionResponse(
                cotizacion.citaId(), cotizacion.especialidad(), cotizacion.montoEstimado(), NOTA_COTIZACION));
    }

    @PatchMapping("/costo-final")
    @PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<PagoResponse> registrarCostoFinal(@PathVariable Long citaId,
                                                            @Valid @RequestBody RegistrarCostoFinalRequest request,
                                                            Authentication authentication) {
        Pago pago = registrarCostoFinalUseCase.registrar(citaId, request.montoFinal(), authentication.getName());
        return ResponseEntity.ok(toResponse(pago));
    }

    @GetMapping("/pago")
    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO', 'ADMINISTRADOR')")
    public ResponseEntity<PagoResponse> consultarPago(@PathVariable Long citaId, Authentication authentication) {
        Pago pago = consultarPagoUseCase.consultar(citaId, authentication.getName());
        return ResponseEntity.ok(toResponse(pago));
    }

    @PatchMapping("/pago/metodo")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PagoResponse> seleccionarMetodo(@PathVariable Long citaId,
                                                          @Valid @RequestBody SeleccionarMetodoPagoRequest request,
                                                          Authentication authentication) {
        Pago pago = seleccionarMetodoPagoUseCase.seleccionar(citaId, request.metodo(), authentication.getName());
        return ResponseEntity.ok(toResponse(pago));
    }

    @PatchMapping("/pago/confirmar-efectivo")
    @PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<PagoResponse> confirmarEfectivo(@PathVariable Long citaId, Authentication authentication) {
        Pago pago = seleccionarMetodoPagoUseCase.confirmarEfectivo(citaId, authentication.getName());
        return ResponseEntity.ok(toResponse(pago));
    }

    @PostMapping("/comprobante")
    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO', 'ADMINISTRADOR')")
    public ResponseEntity<ComprobanteResponse> generarComprobante(@PathVariable Long citaId,
                                                                  Authentication authentication) {
        ComprobantePago comprobante = generarComprobanteUseCase.generar(citaId, authentication.getName());
        return ResponseEntity.ok(toResponse(comprobante));
    }

    @GetMapping("/comprobante")
    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO', 'ADMINISTRADOR')")
    public ResponseEntity<ComprobanteResponse> consultarComprobante(@PathVariable Long citaId,
                                                                    Authentication authentication) {
        ComprobantePago comprobante = generarComprobanteUseCase.consultar(citaId, authentication.getName());
        return ResponseEntity.ok(toResponse(comprobante));
    }

    private PagoResponse toResponse(Pago pago) {
        return new PagoResponse(
                pago.getId(),
                pago.getCita().getId(),
                pago.getMontoCotizado(),
                pago.getMontoFinal(),
                pago.getMetodo() == null ? null : pago.getMetodo().name(),
                pago.getEstado().name(),
                pago.getEstado().getEtiqueta(),
                pago.getReferenciaExterna(),
                pago.getPagadoEn()
        );
    }

    private ComprobanteResponse toResponse(ComprobantePago comprobante) {
        Pago pago = comprobante.getPago();
        Cita cita = pago.getCita();
        return new ComprobanteResponse(
                comprobante.getNumero(),
                comprobante.getEmitidoEn(),
                cita.getId(),
                cita.getEspecialidad().getNombre(),
                nombreCompleto(cita.getCliente().getUsuario()),
                cita.getTecnico() == null ? null : nombreCompleto(cita.getTecnico().getUsuario()),
                pago.getMontoFinal(),
                pago.getMetodo().getEtiqueta(),
                pago.getReferenciaExterna(),
                pago.getPagadoEn()
        );
    }

    private String nombreCompleto(Usuario usuario) {
        return usuario.getNombre() + " " + usuario.getApellido();
    }
}
