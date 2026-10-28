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
import java.util.function.Supplier;

@Component
public class NotificadorCitaHttpAdapter implements NotificadorCitaPort {

    private final String urlNotificaciones;
    private final RegistroLogPort registroLogPort;
    private final DatosCorreoCitaResolver datosCorreo;
    private final HttpClient clienteHttp = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    public NotificadorCitaHttpAdapter(@Value("${notificaciones.url:}") String urlNotificaciones,
                                      RegistroLogPort registroLogPort,
                                      DatosCorreoCitaResolver datosCorreo) {
        this.urlNotificaciones = urlNotificaciones;
        this.registroLogPort = registroLogPort;
        this.datosCorreo = datosCorreo;
    }

    @Override
    public void notificarCitaConfirmada(Cita cita, String correoCliente) {
        enviar("/api/notificaciones/cita-confirmada", correoCliente, () -> {
            DatosCorreoCitaResolver.DatosCorreoCita datos = datosCorreo.resolver(cita);
            return "{\"citaId\":" + cita.getId()
                    + "," + campo("correoCliente", correoCliente)
                    + "," + campo("nombreCliente", datos.nombreCliente())
                    + "," + campo("servicio", datos.servicio())
                    + "," + campo("tecnico", datos.tecnico())
                    + "," + campo("fecha", cita.getFechaHoraInicio() == null ? null : cita.getFechaHoraInicio().toLocalDate().toString())
                    + "," + campo("hora", cita.getFechaHoraInicio() == null ? null : cita.getFechaHoraInicio().toLocalTime().toString())
                    + "," + campo("direccion", cita.getDireccionServicio())
                    + ",\"inicio\":\"" + cita.getFechaHoraInicio() + "\""
                    + ",\"fin\":\"" + cita.getFechaHoraFin() + "\"}";
        });
    }

    @Override
    public void notificarCitaFinalizada(Cita cita, String correoCliente) {
        enviar("/api/notificaciones/cita-finalizada", correoCliente, () -> {
            DatosCorreoCitaResolver.DatosCorreoCita datos = datosCorreo.resolver(cita);
            return "{\"citaId\":" + cita.getId()
                    + "," + campo("correoCliente", correoCliente)
                    + "," + campo("nombreCliente", datos.nombreCliente())
                    + "," + campo("servicio", datos.servicio())
                    + "," + campo("tecnico", datos.tecnico())
                    + "," + campo("diagnostico", cita.getDiagnostico()) + "}";
        });
    }

    @Override
    public void notificarCambioDeCita(Cita cita, String correoDestinatario, String mensaje) {
        enviar("/api/notificaciones/cita-actualizada", correoDestinatario, () -> "{\"citaId\":" + cita.getId()
                + "," + campo("correoDestinatario", correoDestinatario)
                + ",\"estado\":\"" + cita.getEstado() + "\""
                + ",\"inicio\":\"" + cita.getFechaHoraInicio() + "\""
                + "," + campo("mensaje", mensaje) + "}");
    }

    /** El cuerpo se arma dentro del try: si falta algún dato, el fallo se registra y no sube al caso de uso. */
    private void enviar(String ruta, String correoCliente, Supplier<String> armarCuerpo) {
        if (urlNotificaciones == null || urlNotificaciones.isBlank()) {
            return;
        }

        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(urlNotificaciones + ruta))
                    // el microservicio envía el correo SMTP antes de responder, por eso el margen es mayor
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json")
                    .header("Authorization", obtenerAutorizacionActual())
                    .POST(HttpRequest.BodyPublishers.ofString(armarCuerpo.get()))
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

    private String campo(String nombre, String valor) {
        return "\"" + nombre + "\":\"" + escapar(valor) + "\"";
    }

    private String escapar(String texto) {
        return texto == null ? "" : texto.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
