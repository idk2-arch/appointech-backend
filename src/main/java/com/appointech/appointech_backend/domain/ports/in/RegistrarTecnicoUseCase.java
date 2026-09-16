package com.appointech.appointech_backend.domain.ports.in;

import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.TipoDocumento;

public interface RegistrarTecnicoUseCase {
    Tecnico registrar(String nombre, String apellido, String correo, String contrasena, String telefono,
                      TipoDocumento tipoDocumento, String numeroDocumento, String direccionBase,
                      Integer radioCoberturaKm);
}