package yadi.samuraiai.client.voice.core;

import yadi.samuraiai.client.voice.util.VoiceHashVerifier;
import java.nio.file.Path;

public final class VoiceIntegrityChecker {
    public record Result(boolean valid, String detail) {}
    public Result check(Path model, VoiceModelManager.ModelInfo info) {
        try { return VoiceHashVerifier.matches(model, info.sha256(), info.size()) ? new Result(true, "SHA-256 válido") : new Result(false, "Modelo ausente, tamaño incorrecto o hash inválido"); }
        catch (Exception error) { return new Result(false, error.getClass().getSimpleName() + ": " + error.getMessage()); }
    }
}
