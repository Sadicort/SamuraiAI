package yadi.samuraiai.foundation.certification;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.time.Instant;
import java.util.*;

/** Fail-closed certificate: every requirement must PASS against the exact same artifact. */
public final class FoundationValidator {
    public FoundationCertificate validate(Path artifact, Collection<CertificationEvidence> supplied) throws IOException {
        if (!Files.isRegularFile(artifact)) throw new FileNotFoundException(artifact.toString());
        String hash = sha256(artifact);
        EnumMap<FoundationRequirement, CertificationEvidence> evidence = new EnumMap<>(FoundationRequirement.class);
        for (CertificationEvidence item : supplied) {
            if (evidence.putIfAbsent(item.requirement(), item) != null)
                throw new IllegalArgumentException("Duplicate evidence: " + item.requirement());
        }
        List<FoundationRequirement> missing = Arrays.stream(FoundationRequirement.values())
                .filter(requirement -> {
                    CertificationEvidence item = evidence.get(requirement);
                    return item == null || item.status() != EvidenceStatus.PASS || !hash.equalsIgnoreCase(item.artifactSha256());
                }).toList();
        boolean explicitFailure = evidence.values().stream().anyMatch(item -> item.status() == EvidenceStatus.FAIL);
        String state = missing.isEmpty() ? "READY" : explicitFailure ? "BLOCKED" : "PARTIAL";
        return new FoundationCertificate(state, missing.isEmpty(), hash, Instant.now(),
                evidence.values().stream().sorted(Comparator.comparing(item -> item.requirement().ordinal())).toList(), missing);
    }
    public static String sha256(Path artifact) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(artifact)) {
                byte[] buffer = new byte[8192];
                for (int read; (read = input.read(buffer)) >= 0;) if (read > 0) digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest()).toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
