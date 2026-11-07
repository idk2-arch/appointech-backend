package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.ListadoConTotal;
import com.appointech.appointech_backend.domain.models.Notificacion;
import com.appointech.appointech_backend.domain.ports.in.ConsultarNotificacionesUseCase;
import com.appointech.appointech_backend.domain.ports.in.MarcarNotificacionesLeidasUseCase;
import com.appointech.appointech_backend.infrastructure.dto.ConteoNoLeidasResponse;
import com.appointech.appointech_backend.infrastructure.dto.LecturaMasivaResponse;
import com.appointech.appointech_backend.infrastructure.dto.NotificacionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** La campana de la app: cualquier usuario autenticado ve y marca únicamente sus propias notificaciones. */
@RestController
@RequestMapping("/api/notificaciones")
@PreAuthorize("isAuthenticated()")
public class NotificacionController {

    static final String ENCABEZADO_TOTAL = "X-Total-Count";

    private final ConsultarNotificacionesUseCase consultarNotificaciones;
    private final MarcarNotificacionesLeidasUseCase marcarLeidas;

    public NotificacionController(ConsultarNotificacionesUseCase consultarNotificaciones,
                                  MarcarNotificacionesLeidasUseCase marcarLeidas) {
        this.consultarNotificaciones = consultarNotificaciones;
        this.marcarLeidas = marcarLeidas;
    }

    /** El total de notificaciones del filtro va en {@code X-Total-Count}; el cuerpo es solo la página (por defecto, la primera de 20). */
    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> listar(@RequestParam(required = false) String soloNoLeidas,
                                                             @RequestParam(required = false) Integer page,
                                                             @RequestParam(required = false) Integer size,
                                                             Authentication authentication) {
        ListadoConTotal<Notificacion> listado = consultarNotificaciones.listar(authentication.getName(),
                NotificacionMapper.aSoloNoLeidas(soloNoLeidas), NotificacionMapper.aPaginacion(page, size));
        return ResponseEntity.ok()
                .header(ENCABEZADO_TOTAL, String.valueOf(listado.total()))
                .body(listado.elementos().stream().map(NotificacionMapper::aRespuesta).toList());
    }

    @GetMapping("/no-leidas/conteo")
    public ResponseEntity<ConteoNoLeidasResponse> contarNoLeidas(Authentication authentication) {
        return ResponseEntity.ok(new ConteoNoLeidasResponse(consultarNotificaciones.contarNoLeidas(authentication.getName())));
    }

    @PatchMapping("/{id}/leida")
    public ResponseEntity<NotificacionResponse> marcarLeida(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(NotificacionMapper.aRespuesta(marcarLeidas.marcarLeida(authentication.getName(), id)));
    }

    @PatchMapping("/leer-todas")
    public ResponseEntity<LecturaMasivaResponse> marcarTodasLeidas(Authentication authentication) {
        return ResponseEntity.ok(new LecturaMasivaResponse(marcarLeidas.marcarTodasLeidas(authentication.getName())));
    }
}
