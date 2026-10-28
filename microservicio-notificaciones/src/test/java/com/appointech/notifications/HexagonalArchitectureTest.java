package com.appointech.notifications;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The inner layers (domain, application) must not depend on Spring, jakarta.mail or the infrastructure layer. */
class HexagonalArchitectureTest {

    private static final Path BASE = Path.of("src/main/java/com/appointech/notifications");
    private static final List<String> FORBIDDEN = List.of(
            "import org.springframework", "import jakarta.mail", "import com.appointech.notifications.infrastructure");

    private void assertNoForbiddenImports(String layer) throws IOException {
        Path directory = BASE.resolve(layer);
        assertTrue(Files.isDirectory(directory), "no existe la capa " + layer);
        try (Stream<Path> files = Files.walk(directory)) {
            List<Path> javaFiles = files.filter(p -> p.toString().endsWith(".java")).toList();
            assertFalse(javaFiles.isEmpty(), "la capa " + layer + " no tiene clases");
            for (Path file : javaFiles) {
                String source = Files.readString(file);
                for (String forbidden : FORBIDDEN) {
                    assertFalse(source.contains(forbidden), file + " no debe contener: " + forbidden);
                }
            }
        }
    }

    @Test
    void domainDoesNotDependOnFrameworksOrInfrastructure() throws IOException {
        assertNoForbiddenImports("domain");
    }

    @Test
    void applicationDoesNotDependOnFrameworksOrInfrastructure() throws IOException {
        assertNoForbiddenImports("application");
    }
}
