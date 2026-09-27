package yadi.samuraiai.foundation.certification;

import java.time.Instant;
import java.util.List;

public record FoundationCertificate(String state, boolean phase2Unlocked, String artifactSha256,
                                    Instant generatedAt, List<CertificationEvidence> evidence,
                                    List<FoundationRequirement> missing) {
    public FoundationCertificate { evidence = List.copyOf(evidence); missing = List.copyOf(missing); }
}
