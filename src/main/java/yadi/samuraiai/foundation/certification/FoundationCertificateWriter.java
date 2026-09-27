package yadi.samuraiai.foundation.certification;

import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class FoundationCertificateWriter {
    public void write(FoundationCertificate certificate, Path directory) throws IOException {
        Files.createDirectories(directory);
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("state", certificate.state());
        document.put("phase2Unlocked", certificate.phase2Unlocked());
        document.put("artifactSha256", certificate.artifactSha256());
        document.put("generatedAt", certificate.generatedAt().toString());
        document.put("evidence", certificate.evidence().stream().map(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("requirement", item.requirement().name()); row.put("status", item.status().name());
            row.put("artifactSha256", item.artifactSha256()); row.put("detail", item.detail());
            row.put("source", item.source()); row.put("timestamp", item.timestamp().toString()); return row;
        }).toList());
        document.put("missing", certificate.missing().stream().map(Enum::name).toList());
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(document);
        StringBuilder markdown = new StringBuilder("# Foundation Certificate\n\n")
                .append("Estado: **").append(certificate.state()).append("**\n\n")
                .append("Artifact SHA-256: `").append(certificate.artifactSha256()).append("`\n\n")
                .append("Phase 2 unlocked: `").append(certificate.phase2Unlocked()).append("`\n\n")
                .append("## Evidencia\n\n| Requisito | Estado | Fuente | Detalle |\n|---|---|---|---|\n");
        certificate.evidence().forEach(item -> markdown.append('|').append(item.requirement()).append('|')
                .append(item.status()).append('|').append(clean(item.source())).append('|').append(clean(item.detail())).append("|\n"));
        if (!certificate.missing().isEmpty()) markdown.append("\nPendientes: ").append(certificate.missing()).append("\n");
        replace(directory.resolve("FOUNDATION_STATUS.json"), json);
        replace(directory.resolve("FOUNDATION_CERTIFICATE.md"), markdown.toString());
    }
    private static String clean(String value) { return value.replace('|', '/').replace('\n', ' '); }
    private static void replace(Path target, String value) throws IOException {
        Path temp = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temp, value, StandardCharsets.UTF_8);
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
}
