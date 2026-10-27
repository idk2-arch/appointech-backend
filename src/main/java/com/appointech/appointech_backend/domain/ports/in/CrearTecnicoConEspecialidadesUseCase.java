package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.TipoDocumento;

import java.util.List;

public interface CrearTecnicoConEspecialidadesUseCase {
    /** Crea usuario TECNICO + técnico y le asigna las especialidades en una sola operación transaccional. */
    Tecnico crear(String nombre, String apellido, String correo, String contrasena, String telefono,
                  TipoDocumento tipoDocumento, String numeroDocumento, String direccionBase,
                  Integer radioCoberturaKm, Double latitudBase, Double longitudBase,
                  List<Long> especialidadIds);
}
