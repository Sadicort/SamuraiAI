package noppes.npcs.client.controllers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public class MusicController {
   public static MusicController Instance;
   public SoundInstance playing;
   public ResourceLocation playingResource;
   public Entity playingEntity;

   public MusicController() {
      Instance = this;
   }

   public void stopMusic() {
      SoundManager handler = Minecraft.m_91087_().m_91106_();
      if (this.playing != null) {
         handler.m_120399_(this.playing);
      }

      handler.m_120386_(null, SoundSource.MUSIC);
      handler.m_120386_(null, SoundSource.AMBIENT);
      handler.m_120386_(null, SoundSource.RECORDS);
      this.playingResource = null;
      this.playingEntity = null;
      this.playing = null;
   }

   public void playStreaming(String music, Entity entity, boolean isLooping) {
      if (!this.isPlaying(music)) {
         this.stopMusic();
         this.playingEntity = entity;
         this.playingResource = new ResourceLocation(music);
         SoundManager handler = Minecraft.m_91087_().m_91106_();
         this.playing = new SimpleSoundInstance(
            this.playingResource,
            SoundSource.RECORDS,
            4.0F,
            1.0F,
            SoundInstance.m_235150_(),
            isLooping,
            0,
            Attenuation.LINEAR,
            entity.m_20185_(),
            entity.m_20186_(),
            entity.m_20189_(),
            false
         );
         handler.m_120367_(this.playing);
      }
   }

   public void playMusic(String music, Entity entity, boolean isLooping) {
      if (!this.isPlaying(music)) {
         this.stopMusic();
         this.playingResource = new ResourceLocation(music);
         this.playingEntity = entity;
         SoundManager handler = Minecraft.m_91087_().m_91106_();
         this.playing = new SimpleSoundInstance(
            this.playingResource, SoundSource.MUSIC, 1.0F, 1.0F, SoundInstance.m_235150_(), isLooping, 0, Attenuation.NONE, 0.0, 0.0, 0.0, false
         );
         handler.m_120367_(this.playing);
      }
   }

   public boolean isPlaying(String music) {
      ResourceLocation resource = new ResourceLocation(music);
      return this.playingResource != null && this.playingResource.equals(resource) ? Minecraft.m_91087_().m_91106_().m_120403_(this.playing) : false;
   }

   public void playSound(SoundSource cat, String music, BlockPos pos, float volume, float pitch) {
      SimpleSoundInstance rec = new SimpleSoundInstance(
         new ResourceLocation(music),
         cat,
         volume,
         pitch,
         SoundInstance.m_235150_(),
         false,
         0,
         Attenuation.LINEAR,
         pos.m_123341_() + 0.5F,
         pos.m_123342_(),
         pos.m_123343_() + 0.5F,
         false
      );
      Minecraft.m_91087_().m_91106_().m_120367_(rec);
   }
}
