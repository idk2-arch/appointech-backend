package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.RegistroLog;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import com.appointech.appointech_backend.infrastructure.entities.LogSistemaEntity;
import com.appointech.appointech_backend.infrastructure.repositories.LogSistemaJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class RegistroLogAdapter implements RegistroLogPort {

    private final LogSistemaJpaRepository logSistemaJpaRepository;

    public RegistroLogAdapter(LogSistemaJpaRepository logSistemaJpaRepository) {
        this.logSistemaJpaRepository = logSistemaJpaRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(NivelLog nivel, TipoLog tipo, String origen, String usuario, String mensaje) {
        LogSistemaEntity entity = new LogSistemaEntity();
        entity.setNivel(nivel.name());
        entity.setTipo(tipo.name());
        entity.setOrigen(origen);
        entity.setUsuario(usuario);
        entity.setMensaje(mensaje);
        entity.setCreadoEn(LocalDateTime.now());
        logSistemaJpaRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroLog> listar(String nivel, String tipo, int limite) {
        Pageable pagina = PageRequest.of(0, limite);
        List<LogSistemaEntity> encontrados;
        if (nivel != null && tipo != null) {
            encontrados = logSistemaJpaRepository.findByNivelAndTipoOrderByCreadoEnDesc(nivel, tipo, pagina);
        } else if (nivel != null) {
            encontrados = logSistemaJpaRepository.findByNivelOrderByCreadoEnDesc(nivel, pagina);
        } else if (tipo != null) {
            encontrados = logSistemaJpaRepository.findByTipoOrderByCreadoEnDesc(tipo, pagina);
        } else {
            encontrados = logSistemaJpaRepository.findAllByOrderByCreadoEnDesc(pagina);
        }
        return encontrados.stream()
                .map(entity -> new RegistroLog(entity.getId(), entity.getNivel(), entity.getTipo(),
                        entity.getOrigen(), entity.getUsuario(), entity.getMensaje(), entity.getCreadoEn()))
                .toList();
    }

    @Override
    @Transactional
    public int eliminarAnterioresA(LocalDateTime fechaLimite) {
        return logSistemaJpaRepository.eliminarAnterioresA(fechaLimite);
    }
}
