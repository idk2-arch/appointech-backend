package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.SolicitudManualDetalle;
import com.appointech.appointech_backend.domain.ports.in.AsignarSolicitudManualUseCase;
import com.appointech.appointech_backend.domain.ports.in.ConsultarTecnicosDisponiblesUseCase;
import com.appointech.appointech_backend.domain.ports.in.ListarSolicitudesManualesUseCase;
import com.appointech.appointech_backend.domain.ports.in.ObtenerDetalleCitaUseCase;
import com.appointech.appointech_backend.infrastructure.dto.AsignarSolicitudManualRequest;
import com.appointech.appointech_backend.infrastructure.dto.CitaResponse;
import com.appointech.appointech_backend.infrastructure.dto.SolicitudManualAdminResponse;
import com.appointech.appointech_backend.infrastructure.dto.TecnicoParaAsignarResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/solicitudes-manuales")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminSolicitudManualController {

    static final String ENCABEZADO_TOTAL = "X-Total-Count";

    private final ListarSolicitudesManualesUseCase listarSolicitudesManualesUseCase;
    private final AsignarSolicitudManualUseCase asignarSolicitudManualUseCase;
    private final ObtenerDetalleCitaUseCase obtenerDetalleCitaUseCase;
    private final ConsultarTecnicosDisponiblesUseCase consultarTecnicosDisponiblesUseCase;

    public AdminSolicitudManualController(ListarSolicitudesManualesUseCase listarSolicitudesManualesUseCase,
                                          AsignarSolicitudManualUseCase asignarSolicitudManualUseCase,
                                          ObtenerDetalleCitaUseCase obtenerDetalleCitaUseCase,
                                          ConsultarTecnicosDisponiblesUseCase consultarTecnicosDisponiblesUseCase) {
        this.listarSolicitudesManualesUseCase = listarSolicitudesManualesUseCase;
        this.asignarSolicitudManualUseCase = asignarSolicitudManualUseCase;
        this.obtenerDetalleCitaUseCase = obtenerDetalleCitaUseCase;
        this.consultarTecnicosDisponiblesUseCase = consultarTecnicosDisponiblesUseCase;
    }

    /** El total de solicitudes que cumplen el filtro va en {@code X-Total-Count}; el cuerpo es solo la página (o todas si no se pagina). */
    @GetMapping
    public ResponseEntity<List<SolicitudManualAdminResponse>> listar(
            @RequestParam(required = false) String motivo,
            @RequestParam(required = false) Long especialidadId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        ListadoConTotal<SolicitudManualDetalle> listado = listarSolicitudesManualesUseCase.listar(
                SolicitudManualFiltroMapper.aFiltro(motivo, especialidadId, estado, page, size));
        return ResponseEntity.ok()
                .header(ENCABEZADO_TOTAL, String.valueOf(listado.total()))
                .body(listado.elementos().stream().map(SolicitudManualAdminResponseMapper::aRespuesta).toList());
    }

    /** Técnicos que pueden atender la solicitud en el horario indicado, del más cercano al más lejano. */
    @GetMapping("/{id}/tecnicos-disponibles")
    public ResponseEntity<List<TecnicoParaAsignarResponse>> tecnicosDisponibles(
            @PathVariable Long id,
            @RequestParam(required = false) String inicio,
            @RequestParam(required = false) String fin) {
        return ResponseEntity.ok(consultarTecnicosDisponiblesUseCase.consultar(id,
                        TecnicoParaAsignarMapper.aFechaHora("inicio", inicio),
                        TecnicoParaAsignarMapper.aFechaHora("fin", fin)).stream()
                .map(TecnicoParaAsignarMapper::aRespuesta)
                .toList());
    }

    @PatchMapping("/{id}/asignar")
    public ResponseEntity<CitaResponse> asignar(@PathVariable Long id,
                                                @Valid @RequestBody AsignarSolicitudManualRequest request,
                                                Authentication authentication) {
        Cita cita = asignarSolicitudManualUseCase.asignar(authentication.getName(), id, request.tecnicoId(),
                request.fechaHoraInicio(), request.fechaHoraFin());
        return ResponseEntity.ok(CitaResponseMapper.aRespuestaAdmin(obtenerDetalleCitaUseCase.detallar(cita)));
    }
}
