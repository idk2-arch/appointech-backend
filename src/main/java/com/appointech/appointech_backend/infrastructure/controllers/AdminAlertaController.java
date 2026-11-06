package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.SolicitudManualDetalle;
import com.appointech.appointech_backend.domain.ports.in.ConsultarAlertasAdminUseCase;
import com.appointech.appointech_backend.infrastructure.dto.AlertaAdminResponse;
import com.appointech.appointech_backend.infrastructure.dto.AlertasAdminResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/alertas")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminAlertaController {

    static final String TIPO_SIN_TECNICOS = "SIN_TECNICOS_ESPECIALIDAD";

    private final ConsultarAlertasAdminUseCase consultarAlertasAdminUseCase;

    public AdminAlertaController(ConsultarAlertasAdminUseCase consultarAlertasAdminUseCase) {
        this.consultarAlertasAdminUseCase = consultarAlertasAdminUseCase;
    }

    @GetMapping
    public ResponseEntity<AlertasAdminResponse> listar() {
        List<AlertaAdminResponse> alertas = consultarAlertasAdminUseCase.sinTecnicosParaLaEspecialidad().stream()
                .map(AdminAlertaController::aAlerta)
                .toList();
        return ResponseEntity.ok(new AlertasAdminResponse(alertas.size(), alertas));
    }

    private static AlertaAdminResponse aAlerta(SolicitudManualDetalle s) {
        return new AlertaAdminResponse(TIPO_SIN_TECNICOS,
                "No hay técnicos con la especialidad " + s.especialidadNombre() + " para la solicitud " + s.id(),
                s.id(), s.clienteId(), s.clienteNombre(), s.clienteApellido(), s.telefonoContacto(),
                s.especialidadId(), s.especialidadNombre(), s.direccionServicio(), s.creadaEn());
    }
}
