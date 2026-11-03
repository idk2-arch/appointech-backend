package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.PreDiagnosticoInvalidoException;
import com.appointech.appointech_backend.domain.models.PreDiagnostico;
import com.appointech.appointech_backend.domain.ports.in.GenerarPreDiagnosticoUseCase;
import com.appointech.appointech_backend.domain.ports.out.PreDiagnosticoPort;
import org.springframework.stereotype.Service;

/** Solo orquesta: valida la entrada y delega en el puerto. No recibe datos personales del cliente. */
@Service
public class GenerarPreDiagnosticoUseCaseImpl implements GenerarPreDiagnosticoUseCase {

    static final int MAXIMO_DESCRIPCION = 1000;
    static final int MAXIMO_ESPECIALIDAD = 100;

    private final PreDiagnosticoPort preDiagnosticoPort;

    public GenerarPreDiagnosticoUseCaseImpl(PreDiagnosticoPort preDiagnosticoPort) {
        this.preDiagnosticoPort = preDiagnosticoPort;
    }

    @Override
    public PreDiagnostico generar(String descripcionProblema, String especialidad) {
        if (descripcionProblema == null || descripcionProblema.isBlank()) {
            throw new PreDiagnosticoInvalidoException("La descripción del problema es obligatoria");
        }
        if (descripcionProblema.length() > MAXIMO_DESCRIPCION) {
            throw new PreDiagnosticoInvalidoException(
                    "La descripción del problema no puede superar " + MAXIMO_DESCRIPCION + " caracteres");
        }
        if (especialidad != null && especialidad.length() > MAXIMO_ESPECIALIDAD) {
            throw new PreDiagnosticoInvalidoException(
                    "La especialidad no puede superar " + MAXIMO_ESPECIALIDAD + " caracteres");
        }
        String especialidadLimpia = especialidad == null || especialidad.isBlank() ? null : especialidad.trim();
        return preDiagnosticoPort.generar(descripcionProblema.trim(), especialidadLimpia);
    }
}
