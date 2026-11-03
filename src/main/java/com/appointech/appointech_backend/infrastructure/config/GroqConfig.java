package com.appointech.appointech_backend.infrastructure.config;

import com.appointech.appointech_backend.domain.ports.out.PreDiagnosticoPort;
import com.appointech.appointech_backend.domain.ports.out.RedactorTecnicoPort;
import com.appointech.appointech_backend.infrastructure.adapters.GroqChatCliente;
import com.appointech.appointech_backend.infrastructure.adapters.GroqPreDiagnosticoAdapter;
import com.appointech.appointech_backend.infrastructure.adapters.GroqRedactorTecnicoAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** Un solo cliente de Groq (misma configuración por entorno) compartido por los puertos de IA. */
@Configuration
public class GroqConfig {

    @Bean
    public GroqChatCliente groqChatCliente(
            @Value("${groq.api-key:}") String apiKey,
            @Value("${groq.model}") String modelo,
            @Value("${groq.base-url}") String baseUrl,
            @Value("${groq.timeout-ms}") long timeoutMs,
            @Value("${groq.max-tokens}") int maximoTokens) {

        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(timeoutMs));
        fabrica.setReadTimeout(Duration.ofMillis(timeoutMs));
        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(fabrica)
                .build();
        return new GroqChatCliente(restClient, apiKey, modelo, maximoTokens);
    }

    @Bean
    public PreDiagnosticoPort preDiagnosticoPort(GroqChatCliente groq) {
        return new GroqPreDiagnosticoAdapter(groq);
    }

    @Bean
    public RedactorTecnicoPort redactorTecnicoPort(GroqChatCliente groq) {
        return new GroqRedactorTecnicoAdapter(groq);
    }
}
