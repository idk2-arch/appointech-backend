package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.TipoDocumento;
import com.appointech.appointech_backend.domain.ports.in.AsignarEspecialidadesTecnicoUseCase;
import com.appointech.appointech_backend.domain.ports.in.CrearTecnicoConEspecialidadesUseCase;
import com.appointech.appointech_backend.domain.ports.in.RegistrarTecnicoUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CrearTecnicoConEspecialidadesUseCaseImpl implements CrearTecnicoConEspecialidadesUseCase {

    private final RegistrarTecnicoUseCase registrarTecnicoUseCase;
    private final AsignarEspecialidadesTecnicoUseCase asignarEspecialidadesTecnicoUseCase;

    public CrearTecnicoConEspecialidadesUseCaseImpl(RegistrarTecnicoUseCase registrarTecnicoUseCase,
                                                    AsignarEspecialidadesTecnicoUseCase asignarEspecialidadesTecnicoUseCase) {
        this.registrarTecnicoUseCase = registrarTecnicoUseCase;
        this.asignarEspecialidadesTecnicoUseCase = asignarEspecialidadesTecnicoUseCase;
    }

    @Override
    @Transactional
    public Tecnico crear(String nombre, String apellido, String correo, String contrasena, String telefono,
                         TipoDocumento tipoDocumento, String numeroDocumento, String direccionBase,
                         Integer radioCoberturaKm, Double latitudBase, Double longitudBase,
                         List<Long> especialidadIds) {
        Tecnico tecnico = registrarTecnicoUseCase.registrar(nombre, apellido, correo, contrasena, telefono,
                tipoDocumento, numeroDocumento, direccionBase, radioCoberturaKm, latitudBase, longitudBase);

        if (especialidadIds == null || especialidadIds.isEmpty()) {
            return tecnico;
        }
        // Si alguna especialidad no existe se lanza la excepción y la transacción revierte el alta.
        return asignarEspecialidadesTecnicoUseCase.asignar(tecnico.getId(), especialidadIds.stream().distinct().toList());
    }
}
