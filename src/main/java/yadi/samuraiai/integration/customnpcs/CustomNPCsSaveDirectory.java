package yadi.samuraiai.integration.customnpcs;

import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.server.ServerLifecycleHooks;
import java.io.File;
import java.io.UncheckedIOException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Normal class so Forge reobfuscates Minecraft calls independently of the mixin target. */
public final class CustomNPCsSaveDirectory {
    public static File resolve(String child) {
        var server = ServerLifecycleHooks.getCurrentServer();
        Path root = server == null ? Path.of(".") : server.getWorldPath(new LevelResource("customnpcs"));
        Path directory = child == null ? root : root.resolve(child);
        try {
            Files.createDirectories(directory);
            return directory.toFile();
        } catch (IOException error) {
            throw new UncheckedIOException("Cannot prepare CustomNPCs save directory: " + directory, error);
        }
    }
    private CustomNPCsSaveDirectory() { }
}
