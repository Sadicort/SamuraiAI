package yadi.samuraiai.foundation.certification;

import java.time.Instant;
import java.util.Objects;

public record CertificationEvidence(FoundationRequirement requirement, EvidenceStatus status,
                                    String artifactSha256, String detail, String source, Instant timestamp) {
    public CertificationEvidence {
        Objects.requireNonNull(requirement); Objects.requireNonNull(status); Objects.requireNonNull(timestamp);
        artifactSha256 = Objects.requireNonNullElse(artifactSha256, "");
        detail = Objects.requireNonNullElse(detail, ""); source = Objects.requireNonNullElse(source, "");
    }
}
