package yadi.samuraiai.foundation;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import yadi.samuraiai.foundation.certification.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FoundationValidatorTest {
    @TempDir Path directory;
    private Path artifact() throws Exception { Path file=directory.resolve("samuraiai.jar");Files.writeString(file,"artifact");return file; }
    private static CertificationEvidence evidence(FoundationRequirement requirement, EvidenceStatus status, String hash) {
        return new CertificationEvidence(requirement,status,hash,"fixture","test",Instant.now());
    }
    @Test void onlyCompleteEvidenceForExactArtifactUnlocksPhase2() throws Exception {
        Path artifact=artifact();String hash=FoundationValidator.sha256(artifact);
        List<CertificationEvidence> evidence=Arrays.stream(FoundationRequirement.values())
                .map(requirement->evidence(requirement,EvidenceStatus.PASS,hash)).toList();
        var certificate=new FoundationValidator().validate(artifact,evidence);
        assertEquals("READY",certificate.state());assertTrue(certificate.phase2Unlocked());assertTrue(certificate.missing().isEmpty());
    }
    @Test void missingOrWrongArtifactEvidenceRemainsPartial() throws Exception {
        Path artifact=artifact();
        var certificate=new FoundationValidator().validate(artifact,List.of(evidence(FoundationRequirement.AI_QUEUE,EvidenceStatus.PASS,"0".repeat(64))));
        assertEquals("PARTIAL",certificate.state());assertFalse(certificate.phase2Unlocked());
        assertTrue(certificate.missing().contains(FoundationRequirement.AI_QUEUE));
    }
    @Test void explicitFailureBlocksAndDuplicateEvidenceIsRejected() throws Exception {
        Path artifact=artifact();String hash=FoundationValidator.sha256(artifact);
        var failed=evidence(FoundationRequirement.RUNTIME,EvidenceStatus.FAIL,hash);
        assertEquals("BLOCKED",new FoundationValidator().validate(artifact,List.of(failed)).state());
        assertThrows(IllegalArgumentException.class,()->new FoundationValidator().validate(artifact,List.of(failed,failed)));
    }
    @Test void writerProducesMatchingMarkdownAndJson() throws Exception {
        Path artifact=artifact();var certificate=new FoundationValidator().validate(artifact,List.of());
        Path output=directory.resolve("certificate");new FoundationCertificateWriter().write(certificate,output);
        assertEquals("PARTIAL",JsonParser.parseString(Files.readString(output.resolve("FOUNDATION_STATUS.json")))
                .getAsJsonObject().get("state").getAsString());
        assertTrue(Files.readString(output.resolve("FOUNDATION_CERTIFICATE.md")).contains(certificate.artifactSha256()));
    }
}
