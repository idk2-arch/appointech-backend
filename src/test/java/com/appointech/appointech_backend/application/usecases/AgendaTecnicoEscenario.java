package com.appointech.appointech_backend.application.usecases;

import com.appointech.appointech_backend.application.services.ResolverTecnicoAutenticado;
import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.ClienteResumen;
import com.appointech.appointech_backend.domain.models.ConfiguracionRuta;
import com.appointech.appointech_backend.domain.models.Coordenadas;
import com.appointech.appointech_backend.domain.models.Distancia;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.RegistroLog;
import com.appointech.appointech_backend.domain.models.Tecnico;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.models.Usuario;
import com.appointech.appointech_backend.domain.ports.out.BloqueIndisponibilidadRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.CalculadoraDistanciaPort;
import com.appointech.appointech_backend.domain.ports.out.ConsultaCitasDelTecnicoPort;
import com.appointech.appointech_backend.domain.ports.out.ConsultaClientesPort;
import com.appointech.appointech_backend.domain.ports.out.GestionBloquesPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.domain.ports.out.TecnicoRepositoryPort;
import com.appointech.appointech_backend.domain.ports.out.UsuarioRepositoryPort;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Mundo en memoria para probar los casos de uso de la agenda del técnico: todos los puertos son fakes,
 * sin red ni base de datos. Cualquier otra implementación de los puertos debería comportarse igual.
 */
class AgendaTecnicoEscenario {

    static final String CORREO_TECNICO = "tecnico@correo.com";
    static final String CORREO_OTRO = "otro@correo.com";
    static final Coordenadas BASE = new Coordenadas(4.3365, -74.3638);
    static final ZoneId ZONA = ZoneId.of("America/Bogota");
    /** "Ahora" fijo de las pruebas: 2030-01-10 08:00. */
    static final LocalDateTime AHORA = LocalDateTime.of(2030, 1, 10, 8, 0);
    static final LocalDate HOY = AHORA.toLocalDate();
    static final LocalDate MANANA = HOY.plusDays(1);

    final Clock reloj = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
    final List<Cita> citas = new ArrayList<>();
    final List<BloqueIndisponibilidad> bloques = new ArrayList<>();
    final List<String> logs = new ArrayList<>();
    final Map<Long, ClienteResumen> clientes = new HashMap<>();
    final Map<Coordenadas, Distancia> distanciasPorDestino = new HashMap<>();
    final List<Coordenadas> origenesConsultados = new ArrayList<>();
    final List<List<Coordenadas>> destinosConsultados = new ArrayList<>();
    final List<Tecnico> tecnicos = new ArrayList<>();
    final Map<String, Usuario> usuarios = new HashMap<>();
    private long siguienteBloqueId = 1;

    final UsuarioRepositoryPort usuarioRepositoryPort = new UsuarioRepositoryPort() {
        @Override
        public Optional<Usuario> buscarPorNumeroDocumento(String numeroDocumento) {
            return Optional.empty();
        }

        @Override
        public Usuario guardar(Usuario usuario) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Usuario> buscarPorCorreo(String correo) {
            return Optional.ofNullable(usuarios.get(correo));
        }

        @Override
        public Optional<Usuario> buscarPorId(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Usuario> listarTodos() {
            throw new UnsupportedOperationException();
        }
    };

    final TecnicoRepositoryPort tecnicoRepositoryPort = new TecnicoRepositoryPort() {
        @Override
        public Tecnico guardar(Tecnico tecnico) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Tecnico> buscarPorUsuarioId(Long usuarioId) {
            return tecnicos.stream().filter(t -> t.getUsuario().getId().equals(usuarioId)).findFirst();
        }

        @Override
        public Optional<Tecnico> buscarPorId(Long id) {
            return tecnicos.stream().filter(t -> t.getId().equals(id)).findFirst();
        }

        @Override
        public List<Tecnico> listarTodos() {
            return tecnicos;
        }

        @Override
        public List<Tecnico> buscarActivosPorEspecialidad(Long especialidadId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Tecnico> buscarPorIdBloqueando(Long id) {
            return buscarPorId(id);
        }
    };

    final ConsultaCitasDelTecnicoPort consultaCitasPort = (tecnicoId, estados, inicio, fin) -> citas.stream()
            .filter(c -> tecnicoId.equals(c.getTecnicoId()))
            .filter(c -> estados.contains(c.getEstado()))
            .filter(c -> c.getFechaHoraInicio().isBefore(fin) && c.getFechaHoraFin().isAfter(inicio))
            .sorted(Comparator.comparing(Cita::getFechaHoraInicio))
            .toList();

    final ConsultaClientesPort consultaClientesPort = (Collection<Long> ids) -> {
        Map<Long, ClienteResumen> resultado = new HashMap<>();
        ids.forEach(id -> {
            if (clientes.containsKey(id)) {
                resultado.put(id, clientes.get(id));
            }
        });
        return resultado;
    };

    /** Un mismo fake cumple los dos puertos de bloques: lectura para franjas y gestión para el técnico. */
    final class BloquesFake implements GestionBloquesPort, BloqueIndisponibilidadRepositoryPort {
        @Override
        public BloqueIndisponibilidad guardar(BloqueIndisponibilidad bloque) {
            bloque.setId(siguienteBloqueId++);
            bloques.add(bloque);
            return bloque;
        }

        @Override
        public List<BloqueIndisponibilidad> listarDelTecnico(Long tecnicoId, LocalDateTime desde, LocalDateTime hasta) {
            return bloques.stream()
                    .filter(b -> b.getTecnicoId().equals(tecnicoId))
                    .filter(b -> b.getFin().isAfter(desde))
                    .filter(b -> hasta == null || b.getInicio().isBefore(hasta))
                    .toList();
        }

        @Override
        public boolean eliminarDelTecnico(Long bloqueId, Long tecnicoId) {
            return bloques.removeIf(b -> b.getId().equals(bloqueId) && b.getTecnicoId().equals(tecnicoId));
        }

        @Override
        public List<BloqueIndisponibilidad> buscarDelTecnicoEntre(Long tecnicoId, LocalDateTime inicio, LocalDateTime fin) {
            return bloques.stream()
                    .filter(b -> b.getTecnicoId().equals(tecnicoId))
                    .filter(b -> b.getInicio().isBefore(fin) && b.getFin().isAfter(inicio))
                    .toList();
        }
    }

    final BloquesFake bloquesFake = new BloquesFake();

    final RegistroLogPort registroLogPort = new RegistroLogPort() {
        @Override
        public void registrar(NivelLog nivel, TipoLog tipo, String origen, String usuario, String mensaje) {
            logs.add(mensaje);
        }

        @Override
        public List<RegistroLog> listar(String nivel, String tipo, int limite) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int eliminarAnterioresA(LocalDateTime fechaLimite) {
            throw new UnsupportedOperationException();
        }
    };

    final CalculadoraDistanciaPort calculadoraDistanciaPort = (origen, destinos) -> {
        origenesConsultados.add(origen);
        destinosConsultados.add(destinos);
        return destinos.stream().map(d -> distanciasPorDestino.getOrDefault(d, new Distancia(1.0, 5.0))).toList();
    };

    final ResolverTecnicoAutenticado resolverTecnico =
            new ResolverTecnicoAutenticado(usuarioRepositoryPort, tecnicoRepositoryPort);

    AgendaTecnicoEscenario() {
        registrarTecnico(1L, 10L, CORREO_TECNICO, BASE.latitud(), BASE.longitud());
        registrarTecnico(2L, 20L, CORREO_OTRO, 4.5, -74.1);
    }

    void registrarTecnico(Long tecnicoId, Long usuarioId, String correo, Double latitud, Double longitud) {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setCorreo(correo);
        usuario.setActivo(true);
        usuarios.put(correo, usuario);
        tecnicos.removeIf(t -> t.getId().equals(tecnicoId));
        tecnicos.add(new Tecnico(tecnicoId, usuario, "Base", latitud, longitud, 10, List.of()));
    }

    Cita agregarCita(Long id, Long tecnicoId, EstadoCita estado, LocalDateTime inicio, LocalDateTime fin,
                     Double latitud, Double longitud) {
        Cita cita = new Cita();
        cita.setId(id);
        cita.setClienteId(100L + id);
        cita.setTecnicoId(tecnicoId);
        cita.setEspecialidadId(2L);
        cita.setDescripcionProblema("Falla " + id);
        cita.setDireccionServicio("Calle " + id);
        cita.setLatitud(latitud);
        cita.setLongitud(longitud);
        cita.setFechaHoraInicio(inicio);
        cita.setFechaHoraFin(fin);
        cita.setEstado(estado);
        citas.add(cita);
        clientes.put(cita.getClienteId(), new ClienteResumen("Cliente" + id, "Apellido" + id, "300" + id));
        return cita;
    }

    CrearBloqueUseCaseImpl crearBloque() {
        return new CrearBloqueUseCaseImpl(resolverTecnico, consultaCitasPort, bloquesFake, bloquesFake,
                registroLogPort, reloj);
    }

    ListarBloquesUseCaseImpl listarBloques() {
        return new ListarBloquesUseCaseImpl(resolverTecnico, bloquesFake, reloj);
    }

    EliminarBloqueUseCaseImpl eliminarBloque() {
        return new EliminarBloqueUseCaseImpl(resolverTecnico, bloquesFake, registroLogPort);
    }

    ConsultarCitasDelTecnicoUseCaseImpl consultarCitas() {
        return new ConsultarCitasDelTecnicoUseCaseImpl(resolverTecnico, consultaCitasPort, consultaClientesPort);
    }

    CalcularRutaDelDiaUseCaseImpl calcularRuta(int margenMinutos) {
        return calcularRuta(margenMinutos, 0);
    }

    CalcularRutaDelDiaUseCaseImpl calcularRuta(int margenMinutos, int toleranciaMinutos) {
        return new CalcularRutaDelDiaUseCaseImpl(resolverTecnico, consultaCitasPort, consultaClientesPort,
                calculadoraDistanciaPort, new ConfiguracionRuta(margenMinutos, toleranciaMinutos), reloj);
    }
}
