package com.appointech.appointech_backend.infrastructure.adapters;

import com.appointech.appointech_backend.domain.ports.out.GoogleTokenVerifierPort;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de salida: valida la firma, la expiración y la audiencia
 * (que el token fue emitido para NUESTRO Client ID) del ID token de Google.
 */
@Component
public class GoogleTokenVerifierAdapter implements GoogleTokenVerifierPort {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifierAdapter(@Value("${google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(clientId))
                .build();
    }

    @Override
    public Optional<GoogleUser> verificar(String idToken) {
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                return Optional.empty();
            }
            GoogleIdToken.Payload payload = token.getPayload();

            // Solo aceptamos correos que Google ya verificó
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                return Optional.empty();
            }

            String nombre = (String) payload.get("given_name");
            String apellido = (String) payload.get("family_name");

            return Optional.of(new GoogleUser(
                    payload.getSubject(),
                    payload.getEmail(),
                    nombre != null ? nombre : (String) payload.get("name"),
                    apellido != null ? apellido : ""
            ));
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}