package noppes.npcs.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IEntity;

/**
 * Trimmed down from the real CustomNPCs {@code NpcAPI} facade to only the
 * methods {@code yadi.samuraiai.integration.customnpcs.CustomNPCsController} actually
 * calls. This is safe precisely because this class is never shipped (see
 * build.gradle's {@code jar { exclude 'noppes/npcs/api/**' }}): at compile
 * time only these signatures need to resolve, and at runtime the JVM loads
 * the real, complete {@code NpcAPI} from the installed CustomNPCs mod, whose
 * extra methods SamuraiAI simply never calls. Restore methods here (with
 * their real Mojang-mapped signatures) if a future integration needs them.
 */
public abstract class NpcAPI {
   private static NpcAPI instance = null;

   public abstract ICustomNpc createNPC(Level var1);

   public abstract ICustomNpc spawnNPC(Level var1, int var2, int var3, int var4);

   public abstract IEntity getIEntity(Entity var1);

   public abstract IEventBus events();

   public static boolean IsAvailable() {
      return ModList.get().isLoaded("customnpcs");
   }

   public static NpcAPI Instance() {
      if (instance != null) {
         return instance;
      }

      if (!IsAvailable()) {
         return null;
      }

      try {
         Class c = Class.forName("noppes.npcs.api.wrapper.WrapperNpcAPI");
         instance = (NpcAPI)c.getMethod("Instance").invoke(null);
      } catch (Exception e) {
         e.printStackTrace();
      }

      return instance;
   }
}
