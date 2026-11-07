package com.appointech.appointech_backend.domain.ports.out;

import java.util.List;

public interface DestinatariosAdminPort {
    /** Ids de usuario de los administradores activos, a quienes se avisa de las solicitudes especiales. */
    List<Long> idsAdministradoresActivos();
}
