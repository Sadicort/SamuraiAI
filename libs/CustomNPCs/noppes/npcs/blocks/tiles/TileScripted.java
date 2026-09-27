package noppes.npcs.blocks.tiles;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.CustomBlocks;
import noppes.npcs.EventHooks;
import noppes.npcs.NBTTags;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.TextBlock;
import noppes.npcs.api.block.IBlock;
import noppes.npcs.api.block.ITextPlane;
import noppes.npcs.api.wrapper.BlockScriptedWrapper;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.IScriptBlockHandler;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.entity.data.DataTimers;
import noppes.npcs.util.ValueUtil;

public class TileScripted extends TileNpcEntity implements IScriptBlockHandler {
   public List<ScriptContainer> scripts = new ArrayList<>();
   public String scriptLanguage = "ECMAScript";
   public boolean enabled = false;
   private IBlock blockDummy = null;
   public DataTimers timers = new DataTimers(this);
   public long lastInited = -1L;
   private short tickCount = 0;
   public ItemStack itemModel = new ItemStack(CustomBlocks.scripted);
   public Block blockModel = null;
   public boolean needsClientUpdate = false;
   public int powering = 0;
   public int activePowering = 0;
   public int newPower = 0;
   public int prevPower = 0;
   public boolean isPassible = false;
   public boolean isLadder = false;
   public int lightValue = 0;
   public float blockHardness = 5.0F;
   public float blockResistance = 10.0F;
   public int rotationX = 0;
   public int rotationY = 0;
   public int rotationZ = 0;
   public float scaleX = 1.0F;
   public float scaleY = 1.0F;
   public float scaleZ = 1.0F;
   public BlockEntity renderTile;
   public BlockState renderState;
   public boolean renderTileErrored = true;
   public BlockEntityTicker renderTileUpdate = null;
   public TileScripted.TextPlane text1 = new TileScripted.TextPlane();
   public TileScripted.TextPlane text2 = new TileScripted.TextPlane();
   public TileScripted.TextPlane text3 = new TileScripted.TextPlane();
   public TileScripted.TextPlane text4 = new TileScripted.TextPlane();
   public TileScripted.TextPlane text5 = new TileScripted.TextPlane();
   public TileScripted.TextPlane text6 = new TileScripted.TextPlane();

   public TileScripted(BlockPos pos, BlockState state) {
      super(CustomBlocks.tile_scripted, pos, state);
   }

   @Override
   public IBlock getBlock() {
      if (this.blockDummy == null) {
         this.blockDummy = new BlockScriptedWrapper(this.m_58904_(), CustomBlocks.scripted, this.m_58899_());
      }

      return this.blockDummy;
   }

   @Override
   public void m_142466_(CompoundTag compound) {
      super.m_142466_(compound);
      this.setNBT(compound);
      this.setDisplayNBT(compound);
      this.timers.load(compound);
   }

   public void setNBT(CompoundTag compound) {
      this.scripts = NBTTags.GetScript(compound.m_128437_("Scripts", 10), this);
      this.scriptLanguage = compound.m_128461_("ScriptLanguage");
      this.enabled = compound.m_128471_("ScriptEnabled");
      this.activePowering = this.powering = compound.m_128451_("BlockPowering");
      this.prevPower = compound.m_128451_("BlockPrevPower");
      if (compound.m_128441_("BlockHardness")) {
         this.blockHardness = compound.m_128457_("BlockHardness");
         this.blockResistance = compound.m_128457_("BlockResistance");
      }
   }

   public void setDisplayNBT(CompoundTag compound) {
      this.itemModel = ItemStack.m_41712_(compound.m_128469_("ScriptBlockModel"));
      if (this.itemModel.m_41619_()) {
         this.itemModel = new ItemStack(CustomBlocks.scripted);
      }

      if (compound.m_128441_("ScriptBlockModelBlock")) {
         this.blockModel = (Block)ForgeRegistries.BLOCKS.getValue(new ResourceLocation(compound.m_128461_("ScriptBlockModelBlock")));
      }

      this.renderTileUpdate = null;
      this.renderTile = null;
      this.renderTileErrored = false;
      this.lightValue = compound.m_128451_("LightValue");
      this.isLadder = compound.m_128471_("IsLadder");
      this.isPassible = compound.m_128471_("IsPassible");
      this.rotationX = compound.m_128451_("RotationX");
      this.rotationY = compound.m_128451_("RotationY");
      this.rotationZ = compound.m_128451_("RotationZ");
      this.scaleX = compound.m_128457_("ScaleX");
      this.scaleY = compound.m_128457_("ScaleY");
      this.scaleZ = compound.m_128457_("ScaleZ");
      if (this.scaleX <= 0.0F) {
         this.scaleX = 1.0F;
      }

      if (this.scaleY <= 0.0F) {
         this.scaleY = 1.0F;
      }

      if (this.scaleZ <= 0.0F) {
         this.scaleZ = 1.0F;
      }

      if (compound.m_128441_("Text3")) {
         this.text1.setNBT(compound.m_128469_("Text1"));
         this.text2.setNBT(compound.m_128469_("Text2"));
         this.text3.setNBT(compound.m_128469_("Text3"));
         this.text4.setNBT(compound.m_128469_("Text4"));
         this.text5.setNBT(compound.m_128469_("Text5"));
         this.text6.setNBT(compound.m_128469_("Text6"));
      }
   }

   @Override
   public void m_183515_(CompoundTag compound) {
      this.getNBT(compound);
      this.getDisplayNBT(compound);
      this.timers.save(compound);
      super.m_183515_(compound);
   }

   public CompoundTag getNBT(CompoundTag compound) {
      compound.m_128365_("Scripts", NBTTags.NBTScript(this.scripts));
      compound.m_128359_("ScriptLanguage", this.scriptLanguage);
      compound.m_128379_("ScriptEnabled", this.enabled);
      compound.m_128405_("BlockPowering", this.powering);
      compound.m_128405_("BlockPrevPower", this.prevPower);
      compound.m_128350_("BlockHardness", this.blockHardness);
      compound.m_128350_("BlockResistance", this.blockResistance);
      return compound;
   }

   public CompoundTag getDisplayNBT(CompoundTag compound) {
      CompoundTag itemcompound = new CompoundTag();
      this.itemModel.m_41739_(itemcompound);
      if (this.blockModel != null) {
         ResourceLocation resourcelocation = ForgeRegistries.BLOCKS.getKey(this.blockModel);
         compound.m_128359_("ScriptBlockModelBlock", resourcelocation == null ? "" : resourcelocation.toString());
      }

      compound.m_128365_("ScriptBlockModel", itemcompound);
      compound.m_128405_("LightValue", this.lightValue);
      compound.m_128379_("IsLadder", this.isLadder);
      compound.m_128379_("IsPassible", this.isPassible);
      compound.m_128405_("RotationX", this.rotationX);
      compound.m_128405_("RotationY", this.rotationY);
      compound.m_128405_("RotationZ", this.rotationZ);
      compound.m_128350_("ScaleX", this.scaleX);
      compound.m_128350_("ScaleY", this.scaleY);
      compound.m_128350_("ScaleZ", this.scaleZ);
      compound.m_128365_("Text1", this.text1.getNBT());
      compound.m_128365_("Text2", this.text2.getNBT());
      compound.m_128365_("Text3", this.text3.getNBT());
      compound.m_128365_("Text4", this.text4.getNBT());
      compound.m_128365_("Text5", this.text5.getNBT());
      compound.m_128365_("Text6", this.text6.getNBT());
      return compound;
   }

   private boolean isEnabled() {
      return this.enabled && ScriptController.HasStart && !this.f_58857_.f_46443_;
   }

   public static void tick(Level level, BlockPos pos, BlockState state, TileScripted tile) {
      if (tile.renderTileUpdate != null) {
         try {
            tile.renderTileUpdate.m_155252_(level, pos, tile.renderState, tile.renderTile);
         } catch (Exception e) {
            tile.renderTileUpdate = null;
         }
      }

      tile.tickCount++;
      if (tile.prevPower != tile.newPower && tile.powering <= 0) {
         EventHooks.onScriptBlockRedstonePower(tile, tile.prevPower, tile.newPower);
         tile.prevPower = tile.newPower;
      }

      tile.timers.update();
      if (tile.tickCount >= 10) {
         EventHooks.onScriptBlockUpdate(tile);
         tile.tickCount = 0;
         if (tile.needsClientUpdate) {
            tile.m_6596_();
            level.m_7260_(pos, state, state, 3);
            tile.needsClientUpdate = false;
         }
      }
   }

   public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
      this.handleUpdateTag(pkt.m_131708_());
   }

   public void handleUpdateTag(CompoundTag tag) {
      int light = this.lightValue;
      this.setDisplayNBT(tag);
      if (light != this.lightValue) {
         this.f_58857_.m_5518_().m_7174_(this.f_58858_);
      }
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.m_195640_(this);
   }

   public CompoundTag m_5995_() {
      CompoundTag compound = new CompoundTag();
      compound.m_128405_("x", this.f_58858_.m_123341_());
      compound.m_128405_("y", this.f_58858_.m_123342_());
      compound.m_128405_("z", this.f_58858_.m_123343_());
      this.getDisplayNBT(compound);
      return compound;
   }

   public void setItemModel(ItemStack item, Block b) {
      if (item == null || item.m_41619_()) {
         item = new ItemStack(CustomBlocks.scripted);
      }

      if (!NoppesUtilPlayer.compareItems(item, this.itemModel, false, false) || b == this.blockModel) {
         this.itemModel = item;
         this.blockModel = b;
         this.needsClientUpdate = true;
      }
   }

   public void setLightValue(int value) {
      if (value != this.lightValue) {
         this.lightValue = ValueUtil.CorrectInt(value, 0, 15);
         this.needsClientUpdate = true;
      }
   }

   public void setRedstonePower(int strength) {
      if (this.powering != strength) {
         this.prevPower = this.activePowering = ValueUtil.CorrectInt(strength, 0, 15);
         this.f_58857_.m_46672_(this.f_58858_, CustomBlocks.scripted);
         this.powering = this.activePowering;
      }
   }

   public void setScale(float x, float y, float z) {
      if (this.scaleX != x || this.scaleY != y || this.scaleZ != z) {
         this.scaleX = ValueUtil.correctFloat(x, 0.0F, 10.0F);
         this.scaleY = ValueUtil.correctFloat(y, 0.0F, 10.0F);
         this.scaleZ = ValueUtil.correctFloat(z, 0.0F, 10.0F);
         this.needsClientUpdate = true;
      }
   }

   public void setRotation(int x, int y, int z) {
      if (this.rotationX != x || this.rotationY != y || this.rotationZ != z) {
         this.rotationX = ValueUtil.CorrectInt(x, 0, 359);
         this.rotationY = ValueUtil.CorrectInt(y, 0, 359);
         this.rotationZ = ValueUtil.CorrectInt(z, 0, 359);
         this.needsClientUpdate = true;
      }
   }

   @Override
   public void runScript(EnumScriptType type, Event event) {
      if (this.isEnabled()) {
         if (ScriptController.Instance.lastLoaded > this.lastInited) {
            this.lastInited = ScriptController.Instance.lastLoaded;
            if (type != EnumScriptType.INIT) {
               EventHooks.onScriptBlockInit(this);
            }
         }

         for (ScriptContainer script : this.scripts) {
            script.run(type, event);
         }
      }
   }

   @Override
   public boolean isClient() {
      return this.m_58904_().f_46443_;
   }

   @Override
   public boolean getEnabled() {
      return this.enabled;
   }

   @Override
   public void setEnabled(boolean bo) {
      this.enabled = bo;
   }

   @Override
   public String noticeString() {
      BlockPos pos = this.m_58899_();
      return MoreObjects.toStringHelper(this).add("x", pos.m_123341_()).add("y", pos.m_123342_()).add("z", pos.m_123343_()).toString();
   }

   @Override
   public String getLanguage() {
      return this.scriptLanguage;
   }

   @Override
   public void setLanguage(String lang) {
      this.scriptLanguage = lang;
   }

   @Override
   public List<ScriptContainer> getScripts() {
      return this.scripts;
   }

   @Override
   public Map<Long, String> getConsoleText() {
      Map<Long, String> map = new TreeMap<>();
      int tab = 0;

      for (ScriptContainer script : this.getScripts()) {
         tab++;

         for (Entry<Long, String> entry : script.console.entrySet()) {
            map.put(entry.getKey(), " tab " + tab + ":\n" + entry.getValue());
         }
      }

      return map;
   }

   @Override
   public void clearConsole() {
      for (ScriptContainer script : this.getScripts()) {
         script.console.clear();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public AABB getRenderBoundingBox() {
      return Shapes.m_83144_().m_83215_().m_82338_(this.m_58899_());
   }

   public class TextPlane implements ITextPlane {
      public boolean textHasChanged = true;
      public TextBlock textBlock;
      public String text = "";
      public int rotationX = 0;
      public int rotationY = 0;
      public int rotationZ = 0;
      public float offsetX = 0.0F;
      public float offsetY = 0.0F;
      public float offsetZ = 0.5F;
      public float scale = 1.0F;

      @Override
      public String getText() {
         return this.text;
      }

      @Override
      public void setText(String text) {
         if (!this.text.equals(text)) {
            this.text = text;
            this.textHasChanged = true;
            TileScripted.this.needsClientUpdate = true;
         }
      }

      @Override
      public int getRotationX() {
         return this.rotationX;
      }

      @Override
      public int getRotationY() {
         return this.rotationY;
      }

      @Override
      public int getRotationZ() {
         return this.rotationZ;
      }

      @Override
      public void setRotationX(int x) {
         x = ValueUtil.CorrectInt(x % 360, 0, 359);
         if (this.rotationX != x) {
            this.rotationX = x;
            TileScripted.this.needsClientUpdate = true;
         }
      }

      @Override
      public void setRotationY(int y) {
         y = ValueUtil.CorrectInt(y % 360, 0, 359);
         if (this.rotationY != y) {
            this.rotationY = y;
            TileScripted.this.needsClientUpdate = true;
         }
      }

      @Override
      public void setRotationZ(int z) {
         z = ValueUtil.CorrectInt(z % 360, 0, 359);
         if (this.rotationZ != z) {
            this.rotationZ = z;
            TileScripted.this.needsClientUpdate = true;
         }
      }

      @Override
      public float getOffsetX() {
         return this.offsetX;
      }

      @Override
      public float getOffsetY() {
         return this.offsetY;
      }

      @Override
      public float getOffsetZ() {
         return this.offsetZ;
      }

      @Override
      public void setOffsetX(float x) {
         x = ValueUtil.correctFloat(x, -1.0F, 1.0F);
         if (this.offsetX != x) {
            this.offsetX = x;
            TileScripted.this.needsClientUpdate = true;
         }
      }

      @Override
      public void setOffsetY(float y) {
         y = ValueUtil.correctFloat(y, -1.0F, 1.0F);
         if (this.offsetY != y) {
            this.offsetY = y;
            TileScripted.this.needsClientUpdate = true;
         }
      }

      @Override
      public void setOffsetZ(float z) {
         z = ValueUtil.correctFloat(z, -1.0F, 1.0F);
         if (this.offsetZ != z) {
            System.out.println(this.rotationZ);
            this.offsetZ = z;
            TileScripted.this.needsClientUpdate = true;
         }
      }

      @Override
      public float getScale() {
         return this.scale;
      }

      @Override
      public void setScale(float scale) {
         if (this.scale != scale) {
            this.scale = scale;
            TileScripted.this.needsClientUpdate = true;
         }
      }

      public CompoundTag getNBT() {
         CompoundTag compound = new CompoundTag();
         compound.m_128359_("Text", this.text);
         compound.m_128405_("RotationX", this.rotationX);
         compound.m_128405_("RotationY", this.rotationY);
         compound.m_128405_("RotationZ", this.rotationZ);
         compound.m_128350_("OffsetX", this.offsetX);
         compound.m_128350_("OffsetY", this.offsetY);
         compound.m_128350_("OffsetZ", this.offsetZ);
         compound.m_128350_("Scale", this.scale);
         return compound;
      }

      public void setNBT(CompoundTag compound) {
         this.setText(compound.m_128461_("Text"));
         this.rotationX = compound.m_128451_("RotationX");
         this.rotationY = compound.m_128451_("RotationY");
         this.rotationZ = compound.m_128451_("RotationZ");
         this.offsetX = compound.m_128457_("OffsetX");
         this.offsetY = compound.m_128457_("OffsetY");
         this.offsetZ = compound.m_128457_("OffsetZ");
         this.scale = compound.m_128457_("Scale");
      }
   }
}
