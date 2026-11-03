package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.exception.PreDiagnosticoInvalidoException;
import com.appointech.appointech_backend.domain.models.RedaccionTecnica;
import com.appointech.appointech_backend.domain.ports.in.RedactarDescripcionTecnicaUseCase;
import com.appointech.appointech_backend.domain.ports.out.RedactorTecnicoPort;
import org.springframework.stereotype.Service;

/** Solo orquesta: valida la entrada y delega en el puerto. No recibe datos personales del cliente. */
@Service
public class RedactarDescripcionTecnicaUseCaseImpl implements RedactarDescripcionTecnicaUseCase {

    private final RedactorTecnicoPort redactorTecnicoPort;

    public RedactarDescripcionTecnicaUseCaseImpl(RedactorTecnicoPort redactorTecnicoPort) {
        this.redactorTecnicoPort = redactorTecnicoPort;
    }

    @Override
    public RedaccionTecnica redactar(String descripcionProblema, String especialidad) {
        exigirTexto(descripcionProblema, "La descripción del problema", GenerarPreDiagnosticoUseCaseImpl.MAXIMO_DESCRIPCION);
        if (especialidad != null) {
            exigirLargoMaximo(especialidad, "La especialidad", GenerarPreDiagnosticoUseCaseImpl.MAXIMO_ESPECIALIDAD);
        }
        String especialidadLimpia = especialidad == null || especialidad.isBlank() ? null : especialidad.trim();
        return redactorTecnicoPort.redactar(descripcionProblema.trim(), especialidadLimpia);
    }

    private void exigirTexto(String valor, String nombre, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new PreDiagnosticoInvalidoException(nombre + " es obligatoria");
        }
        exigirLargoMaximo(valor, nombre, maximo);
    }

    private void exigirLargoMaximo(String valor, String nombre, int maximo) {
        if (valor.length() > maximo) {
            throw new PreDiagnosticoInvalidoException(nombre + " no puede superar " + maximo + " caracteres");
        }
    }
}
