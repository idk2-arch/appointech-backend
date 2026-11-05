package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.Cliente;
import com.appointech.appointech_backend.domain.models.Especialidad;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.HistorialEstadoCita;
import com.appointech.appointech_backend.domain.models.RolUsuario;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.CitaRepositoryPort;
import com.appointech.appointech_backend.infrastructure.entities.CitaEntity;
import com.appointech.appointech_backend.infrastructure.entities.ClienteEntity;
import com.appointech.appointech_backend.infrastructure.entities.EspecialidadEntity;
import com.appointech.appointech_backend.infrastructure.entities.EstadoCitaEntity;
import com.appointech.appointech_backend.infrastructure.entities.HistorialEstadoCitaEntity;
import com.appointech.appointech_backend.infrastructure.entities.TecnicoEntity;
import com.appointech.appointech_backend.infrastructure.entities.UsuarioEntity;
import com.appointech.appointech_backend.infrastructure.repositories.CitaJpaRepository;
import com.appointech.appointech_backend.infrastructure.repositories.ClienteJpaRepository;
import com.appointech.appointech_backend.infrastructure.repositories.EspecialidadJpaRepository;
import com.appointech.appointech_backend.infrastructure.repositories.HistorialEstadoCitaJpaRepository;
import com.appointech.appointech_backend.infrastructure.repositories.TecnicoJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class CitaRepositoryAdapter implements CitaRepositoryPort {

    private static final List<EstadoCitaEntity> ESTADOS_INACTIVOS =
            List.of(EstadoCitaEntity.CANCELADA, EstadoCitaEntity.FINALIZADA);

    private final CitaJpaRepository citaJpaRepository;
    private final HistorialEstadoCitaJpaRepository historialJpaRepository;
    private final ClienteJpaRepository clienteJpaRepository;
    private final TecnicoJpaRepository tecnicoJpaRepository;
    private final EspecialidadJpaRepository especialidadJpaRepository;

    public CitaRepositoryAdapter(CitaJpaRepository citaJpaRepository,
                                 HistorialEstadoCitaJpaRepository historialJpaRepository,
                                 ClienteJpaRepository clienteJpaRepository,
                                 TecnicoJpaRepository tecnicoJpaRepository,
                                 EspecialidadJpaRepository especialidadJpaRepository) {
        this.citaJpaRepository = citaJpaRepository;
        this.historialJpaRepository = historialJpaRepository;
        this.clienteJpaRepository = clienteJpaRepository;
        this.tecnicoJpaRepository = tecnicoJpaRepository;
        this.especialidadJpaRepository = especialidadJpaRepository;
    }

    @Override
    public Cita guardar(Cita cita) {
        CitaEntity entity = cita.getId() == null
                ? new CitaEntity()
                : citaJpaRepository.findById(cita.getId()).orElseGet(CitaEntity::new);

        entity.setCliente(clienteJpaRepository.getReferenceById(cita.getCliente().getId()));
        entity.setTecnico(cita.getTecnico() == null ? null
                : tecnicoJpaRepository.getReferenceById(cita.getTecnico().getId()));
        entity.setEspecialidad(especialidadJpaRepository.getReferenceById(cita.getEspecialidad().getId()));
        entity.setDescripcionProblema(cita.getDescripcionProblema());
        entity.setDireccionServicio(cita.getDireccionServicio());
        entity.setLatitud(cita.getLatitud());
        entity.setLongitud(cita.getLongitud());
        entity.setFechaHoraInicio(cita.getFechaHoraInicio());
        entity.setFechaHoraFin(cita.getFechaHoraFin());
        entity.setEstado(EstadoCitaEntity.valueOf(cita.getEstado().name()));
        entity.setDiagnostico(cita.getDiagnostico());
        entity.setCreadaEn(cita.getCreadaEn() != null ? cita.getCreadaEn()
                : entity.getCreadaEn() != null ? entity.getCreadaEn() : LocalDateTime.now());
        entity.setActualizadaEn(cita.getActualizadaEn());

        return toDomain(citaJpaRepository.save(entity));
    }

    @Override
    public Optional<Cita> buscarPorId(Long id) {
        return citaJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Cita> listarPorClienteUsuarioId(Long usuarioId) {
        return citaJpaRepository.findByClienteUsuarioIdOrderByCreadaEnDesc(usuarioId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Cita> listarPorTecnicoUsuarioId(Long usuarioId) {
        return citaJpaRepository.findByTecnicoUsuarioIdOrderByFechaHoraInicioAsc(usuarioId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Cita> listarTodas() {
        return citaJpaRepository.findAllByOrderByCreadaEnDesc().stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existeCruceConOtraCita(Long tecnicoId, LocalDateTime inicio, LocalDateTime fin, Long excluirCitaId) {
        long cruces = citaJpaRepository.contarCruces(
                tecnicoId, excluirCitaId == null ? -1L : excluirCitaId, ESTADOS_INACTIVOS, inicio, fin);
        return cruces > 0;
    }

    @Override
    public boolean existeIndisponibilidad(Long tecnicoId, LocalDateTime inicio, LocalDateTime fin) {
        return citaJpaRepository.contarIndisponibilidades(tecnicoId, inicio, fin) > 0;
    }

    @Override
    public HistorialEstadoCita guardarHistorial(HistorialEstadoCita historial) {
        HistorialEstadoCitaEntity entity = new HistorialEstadoCitaEntity();
        entity.setCita(citaJpaRepository.getReferenceById(historial.getCitaId()));
        entity.setEstadoAnterior(historial.getEstadoAnterior() == null ? null
                : EstadoCitaEntity.valueOf(historial.getEstadoAnterior().name()));
        entity.setEstadoNuevo(EstadoCitaEntity.valueOf(historial.getEstadoNuevo().name()));
        if (historial.getUsuario() != null) {
            UsuarioEntity usuario = new UsuarioEntity();
            usuario.setId(historial.getUsuario().getId());
            entity.setUsuario(usuario);
        }
        entity.setComentario(historial.getComentario());
        entity.setCambiadoEn(historial.getCambiadoEn() != null ? historial.getCambiadoEn() : LocalDateTime.now());

        return toDomain(historialJpaRepository.save(entity));
    }

    @Override
    public List<HistorialEstadoCita> listarHistorial(Long citaId) {
        return historialJpaRepository.findByCitaIdOrderByCambiadoEnAscIdAsc(citaId)
                .stream().map(this::toDomain).toList();
    }

    private Cita toDomain(CitaEntity entity) {
        Cita cita = new Cita();
        cita.setId(entity.getId());
        cita.setCliente(toDomainCliente(entity.getCliente()));
        cita.setTecnico(entity.getTecnico() == null ? null : toDomainTecnico(entity.getTecnico()));
        cita.setEspecialidad(toDomainEspecialidad(entity.getEspecialidad()));
        cita.setDescripcionProblema(entity.getDescripcionProblema());
        cita.setDireccionServicio(entity.getDireccionServicio());
        cita.setLatitud(entity.getLatitud());
        cita.setLongitud(entity.getLongitud());
        cita.setFechaHoraInicio(entity.getFechaHoraInicio());
        cita.setFechaHoraFin(entity.getFechaHoraFin());
        cita.setEstado(EstadoCita.valueOf(entity.getEstado().name()));
        cita.setDiagnostico(entity.getDiagnostico());
        cita.setCreadaEn(entity.getCreadaEn());
        cita.setActualizadaEn(entity.getActualizadaEn());
        return cita;
    }

    private HistorialEstadoCita toDomain(HistorialEstadoCitaEntity entity) {
        HistorialEstadoCita historial = new HistorialEstadoCita();
        historial.setId(entity.getId());
        historial.setCitaId(entity.getCita().getId());
        historial.setEstadoAnterior(entity.getEstadoAnterior() == null ? null
                : EstadoCita.valueOf(entity.getEstadoAnterior().name()));
        historial.setEstadoNuevo(EstadoCita.valueOf(entity.getEstadoNuevo().name()));
        historial.setUsuario(entity.getUsuario() == null ? null : toDomainUsuario(entity.getUsuario()));
        historial.setComentario(entity.getComentario());
        historial.setCambiadoEn(entity.getCambiadoEn());
        return historial;
    }

    private Cliente toDomainCliente(ClienteEntity entity) {
        Cliente cliente = new Cliente();
        cliente.setId(entity.getId());
        cliente.setDireccion(entity.getDireccion());
        cliente.setLatitud(entity.getLatitud());
        cliente.setLongitud(entity.getLongitud());
        cliente.setUsuario(toDomainUsuario(entity.getUsuario()));
        return cliente;
    }

    private Tecnico toDomainTecnico(TecnicoEntity entity) {
        Tecnico tecnico = new Tecnico();
        tecnico.setId(entity.getId());
        tecnico.setDireccionBase(entity.getDireccionBase());
        tecnico.setLatitudBase(entity.getLatitudBase());
        tecnico.setLongitudBase(entity.getLongitudBase());
        tecnico.setRadioCoberturaKm(entity.getRadioCoberturaKm());
        tecnico.setEspecialidades(List.of());
        tecnico.setUsuario(toDomainUsuario(entity.getUsuario()));
        return tecnico;
    }

    private Especialidad toDomainEspecialidad(EspecialidadEntity entity) {
        Especialidad especialidad = new Especialidad();
        especialidad.setId(entity.getId());
        especialidad.setNombre(entity.getNombre());
        especialidad.setDescripcion(entity.getDescripcion());
        especialidad.setActivo(entity.isActivo());
        especialidad.setCostoReferencia(entity.getCostoReferencia());
        return especialidad;
    }

    private Usuario toDomainUsuario(UsuarioEntity entity) {
        Usuario usuario = new Usuario();
        usuario.setId(entity.getId());
        usuario.setNombre(entity.getNombre());
        usuario.setApellido(entity.getApellido());
        usuario.setCorreo(entity.getCorreo());
        usuario.setTelefono(entity.getTelefono());
        usuario.setRol(RolUsuario.valueOf(entity.getRol().name()));
        usuario.setActivo(entity.isActivo());
        usuario.setCreadoEn(entity.getCreadoEn());
        return usuario;
    }
}
