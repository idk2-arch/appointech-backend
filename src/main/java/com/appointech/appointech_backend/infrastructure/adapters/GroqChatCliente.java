package com.appointech.appointech_backend.infrastructure.adapters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cliente HTTP común del API de Groq (compatible con OpenAI) que usan los adaptadores de IA.
 * Nunca lanza excepciones: ante llave vacía, error HTTP, timeout o respuesta inválida devuelve vacío.
 * La llave solo viaja en el encabezado Authorization; ni ella ni el texto del cliente se registran en logs.
 */
public class GroqChatCliente {

    private static final Logger log = LoggerFactory.getLogger(GroqChatCliente.class);
    private static final String RUTA_COMPLETIONS = "/chat/completions";

    private final RestClient restClient;
    private final String apiKey;
    private final String modelo;
    private final int maximoTokens;

    public GroqChatCliente(RestClient restClient, String apiKey, String modelo, int maximoTokens) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.modelo = modelo;
        this.maximoTokens = maximoTokens;
    }

    /** Mensaje de usuario que se envía al modelo: solo la especialidad (si hay) y la descripción del problema. */
    public static String mensajeDelProblema(String descripcionProblema, String especialidad) {
        StringBuilder mensaje = new StringBuilder();
        if (especialidad != null) {
            mensaje.append("Especialidad: ").append(especialidad).append('\n');
        }
        return mensaje.append("<problema>").append(descripcionProblema).append("</problema>").toString();
    }

    public Optional<String> completar(String mensajeSistema, String mensajeUsuario, double temperatura) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GROQ_API_KEY no está configurada; la función de IA no está disponible");
            return Optional.empty();
        }
        try {
            Map<String, Object> respuesta = restClient.post()
                    .uri(RUTA_COMPLETIONS)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(construirCuerpo(mensajeSistema, mensajeUsuario, temperatura))
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() { });
            String texto = extraerTexto(respuesta);
            if (!texto.isBlank()) {
                return Optional.of(texto);
            }
            log.warn("Groq respondió sin texto utilizable; la función de IA no está disponible");
        } catch (RestClientResponseException e) {
            log.warn("Groq respondió HTTP {}; la función de IA no está disponible", e.getStatusCode().value());
        } catch (RuntimeException e) {
            log.warn("Falló la llamada a Groq ({}); la función de IA no está disponible",
                    e.getClass().getSimpleName());
        }
        return Optional.empty();
    }

    private Map<String, Object> construirCuerpo(String mensajeSistema, String mensajeUsuario, double temperatura) {
        return Map.of(
                "model", modelo,
                "max_tokens", maximoTokens,
                "temperature", temperatura,
                "messages", List.of(
                        Map.of("role", "system", "content", mensajeSistema),
                        Map.of("role", "user", "content", mensajeUsuario)));
    }

    /** Solo el texto de la primera opción; vacío si la forma de la respuesta no es la esperada. */
    private String extraerTexto(Map<String, Object> respuesta) {
        if (respuesta == null || !(respuesta.get("choices") instanceof List<?> opciones) || opciones.isEmpty()
                || !(opciones.get(0) instanceof Map<?, ?> opcion)
                || !(opcion.get("message") instanceof Map<?, ?> mensaje)
                || !(mensaje.get("content") instanceof String contenido)) {
            return "";
        }
        return contenido.trim();
    }
}
