package yadi.samuraiai.integration.customnpcs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yadi.samuraiai.integration.customnpcs.CustomNPCsSaveDirectory;
import java.io.File;

/** The affected upstream method touches Minecraft's client singleton on a dedicated server. */
@Pseudo
@Mixin(targets = "noppes.npcs.CustomNpcs", remap = false)
public abstract class CustomNPCsSaveDirectoryMixin {
    @Inject(method = "getLevelSaveDirectory(Ljava/lang/String;)Ljava/io/File;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void samuraiai$serverSaveDirectory(String child, CallbackInfoReturnable<File> callback) {
        callback.setReturnValue(CustomNPCsSaveDirectory.resolve(child));
    }
}
