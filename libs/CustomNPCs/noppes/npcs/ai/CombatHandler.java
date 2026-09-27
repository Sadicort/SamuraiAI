package noppes.npcs.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.ability.AbstractAbility;
import noppes.npcs.entity.EntityNPCInterface;

public class CombatHandler {
   private Map<LivingEntity, Float> aggressors = new HashMap<>();
   private EntityNPCInterface npc;
   private long startTime = 0L;
   private int combatResetTimer = 0;

   public CombatHandler(EntityNPCInterface npc) {
      this.npc = npc;
   }

   public void update() {
      if (this.npc.isKilled()) {
         if (this.npc.isAttacking()) {
            this.reset();
         }
      } else {
         if (this.npc.m_5448_() != null && !this.npc.isAttacking()) {
            this.start();
         }

         if (!this.shouldCombatContinue()) {
            if (this.combatResetTimer++ > 40) {
               this.reset();
            }
         } else {
            this.combatResetTimer = 0;
         }
      }
   }

   private boolean shouldCombatContinue() {
      return this.npc.m_5448_() == null ? false : this.isValidTarget(this.npc.m_5448_());
   }

   public void damage(DamageSource source, float damageAmount) {
      this.combatResetTimer = 0;
      if (NoppesUtilServer.GetDamageSourcee(source) instanceof LivingEntity el) {
         Float f = this.aggressors.get(el);
         if (f == null) {
            f = 0.0F;
         }

         this.aggressors.put(el, f + damageAmount);
      }
   }

   public void start() {
      this.combatResetTimer = 0;
      this.startTime = this.npc.f_19853_.m_6106_().m_6793_();
      this.npc.m_20088_().m_135381_(EntityNPCInterface.Attacking, true);

      for (AbstractAbility ab : this.npc.abilities.abilities) {
         ab.startCombat();
      }
   }

   public void reset() {
      this.combatResetTimer = 0;
      this.aggressors.clear();
      this.npc.m_20088_().m_135381_(EntityNPCInterface.Attacking, false);
   }

   public boolean checkTarget() {
      if (!this.aggressors.isEmpty() && this.npc.f_19797_ % 10 == 0) {
         LivingEntity target = this.npc.m_5448_();
         Float current = 0.0F;
         if (this.isValidTarget(target)) {
            current = this.aggressors.get(target);
            if (current == null) {
               current = 0.0F;
            }
         } else {
            target = null;
         }

         for (Entry<LivingEntity, Float> entry : this.aggressors.entrySet()) {
            if (entry.getValue() > current && this.isValidTarget(entry.getKey())) {
               current = entry.getValue();
               target = entry.getKey();
            }
         }

         return target == null;
      } else {
         return false;
      }
   }

   public boolean isValidTarget(LivingEntity target) {
      if (target == null || !target.m_6084_()) {
         return false;
      } else {
         return target instanceof Player && ((Player)target).m_150110_().f_35934_ ? false : this.npc.isInRange(target, this.npc.stats.aggroRange);
      }
   }
}
