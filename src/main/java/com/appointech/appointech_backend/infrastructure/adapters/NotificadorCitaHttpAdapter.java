package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.NivelLog;
import com.appointech.appointech_backend.domain.models.TipoLog;
import com.appointech.appointech_backend.domain.ports.out.NotificadorCitaPort;
import com.appointech.appointech_backend.domain.ports.out.RegistroLogPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class NotificadorCitaHttpAdapter implements NotificadorCitaPort {

    private final String urlNotificaciones;
    private final RegistroLogPort registroLogPort;
    private final HttpClient clienteHttp = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    public NotificadorCitaHttpAdapter(@Value("${notificaciones.url:}") String urlNotificaciones,
                                      RegistroLogPort registroLogPort) {
        this.urlNotificaciones = urlNotificaciones;
        this.registroLogPort = registroLogPort;
    }

    @Override
    public void notificarCitaConfirmada(Cita cita, String correoCliente) {
        String cuerpo = "{\"citaId\":" + cita.getId()
                + ",\"correoCliente\":\"" + escapar(correoCliente) + "\""
                + ",\"inicio\":\"" + cita.getFechaHoraInicio() + "\""
                + ",\"fin\":\"" + cita.getFechaHoraFin() + "\"}";
        enviar("/api/notificaciones/cita-confirmada", cuerpo, correoCliente);
    }

    @Override
    public void notificarCambioDeCita(Cita cita, String correoDestinatario, String mensaje) {
        String cuerpo = "{\"citaId\":" + cita.getId()
                + ",\"correoDestinatario\":\"" + escapar(correoDestinatario) + "\""
                + ",\"estado\":\"" + cita.getEstado() + "\""
                + ",\"inicio\":\"" + cita.getFechaHoraInicio() + "\""
                + ",\"mensaje\":\"" + escapar(mensaje) + "\"}";
        enviar("/api/notificaciones/cita-actualizada", cuerpo, correoDestinatario);
    }

    private void enviar(String ruta, String cuerpo, String correoCliente) {
        if (urlNotificaciones == null || urlNotificaciones.isBlank()) {
            return;
        }

        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(urlNotificaciones + ruta))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .header("Authorization", obtenerAutorizacionActual())
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                    .build();

            HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() >= 300) {
                registroLogPort.registrar(NivelLog.WARN, TipoLog.INTEGRACION, "NotificadorCitaHttpAdapter",
                        correoCliente, "El microservicio de notificaciones respondió " + respuesta.statusCode());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            registroLogPort.registrar(NivelLog.WARN, TipoLog.INTEGRACION, "NotificadorCitaHttpAdapter",
                    correoCliente, "No se pudo contactar al microservicio de notificaciones: " + ex.getMessage());
        }
    }

    private String obtenerAutorizacionActual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            String autorizacion = atributos.getRequest().getHeader("Authorization");
            return autorizacion == null ? "" : autorizacion;
        }
        return "";
    }

    private String escapar(String texto) {
        return texto == null ? "" : texto.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
