package yadi.samuraiai.client.voice.util;

import java.io.*;
import java.nio.file.*;
import java.security.*;

public final class VoiceHashVerifier {
    public static String sha256(Path path) throws IOException { try { MessageDigest digest = MessageDigest.getInstance("SHA-256"); try (InputStream input = Files.newInputStream(path)) { byte[] buffer = new byte[8192]; for (int count; (count = input.read(buffer)) >= 0;) if (count > 0) digest.update(buffer, 0, count); } StringBuilder result = new StringBuilder(); for (byte value : digest.digest()) result.append(String.format("%02x", value)); return result.toString(); } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); } }
    public static boolean matches(Path path, String expected, long size) throws IOException { return Files.exists(path) && (size <= 0 || Files.size(path) == size) && expected != null && !expected.isBlank() && expected.equalsIgnoreCase(sha256(path)); }
    private VoiceHashVerifier() {}
}
