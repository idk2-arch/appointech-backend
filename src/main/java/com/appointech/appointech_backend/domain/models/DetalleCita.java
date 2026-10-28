package com.appointech.appointech_backend.domain.models;

/**
 * Modelo de lectura: una cita junto con los datos del técnico que la atiende y de su especialidad.
 * Los datos del técnico y la especialidad son null cuando la cita aún no tiene técnico (solicitud pendiente).
 */
public record DetalleCita(
        Cita cita,
        String tecnicoNombre,
        String tecnicoApellido,
        String tecnicoTelefono,
        String especialidadNombre,
        String tecnicoFotoClave
) {

    /** Detalle sin foto del técnico. */
    public DetalleCita(Cita cita, String tecnicoNombre, String tecnicoApellido, String tecnicoTelefono,
                       String especialidadNombre) {
        this(cita, tecnicoNombre, tecnicoApellido, tecnicoTelefono, especialidadNombre, null);
    }
}
