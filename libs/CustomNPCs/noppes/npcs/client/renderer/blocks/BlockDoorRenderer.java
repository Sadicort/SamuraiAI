package noppes.npcs.client.renderer.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.client.model.data.ModelData;
import noppes.npcs.CustomBlocks;
import noppes.npcs.CustomItems;
import noppes.npcs.blocks.tiles.TileDoor;

public class BlockDoorRenderer extends BlockRendererInterface<TileDoor> {
   private static Random random = new Random();

   public BlockDoorRenderer(Context dispatcher) {
      super(dispatcher);
   }

   public void render(TileDoor tile, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {
      BlockState original = tile.m_58904_().m_8055_(tile.m_58899_());
      if (!original.m_60795_()) {
         BlockPos lowerPos = tile.m_58899_();
         if (original.m_61143_(DoorBlock.f_52730_) == DoubleBlockHalf.UPPER) {
            lowerPos = tile.m_58899_().m_7495_();
         }

         BlockPos upperPos = lowerPos.m_7494_();
         TileDoor lowerTile = (TileDoor)tile.m_58904_().m_7702_(lowerPos);
         TileDoor upperTile = (TileDoor)tile.m_58904_().m_7702_(upperPos);
         if (lowerTile != null && upperTile != null) {
            BlockState lowerState = lowerTile.m_58900_();
            BlockState upperState = upperTile.m_58900_();
            Block b = lowerTile.blockModel;
            if (this.overrideModel()) {
               b = CustomBlocks.scripted_door;
            }

            BlockState state = b.m_49966_();
            state = (BlockState)state.m_61124_(DoorBlock.f_52730_, (DoubleBlockHalf)original.m_61143_(DoorBlock.f_52730_));
            state = (BlockState)state.m_61124_(DoorBlock.f_52726_, (Direction)lowerState.m_61143_(DoorBlock.f_52726_));
            state = (BlockState)state.m_61124_(DoorBlock.f_52727_, (Boolean)lowerState.m_61143_(DoorBlock.f_52727_));
            state = (BlockState)state.m_61124_(DoorBlock.f_52728_, (DoorHingeSide)upperState.m_61143_(DoorBlock.f_52728_));
            state = (BlockState)state.m_61124_(DoorBlock.f_52729_, (Boolean)upperState.m_61143_(DoorBlock.f_52729_));
            matrixStack.m_85836_();
            this.renderBlock(matrixStack, buffer, tile, lowerState.m_60734_(), state, light, overlay);
            matrixStack.m_85849_();
         }
      }
   }

   private void renderBlock(PoseStack matrixStack, MultiBufferSource buffer, TileDoor tile, Block b, BlockState state, int light, int overlay) {
      BlockRenderDispatcher dispatcher = Minecraft.m_91087_().m_91289_();
      BakedModel ibakedmodel = dispatcher.m_110910_(state);
      if (ibakedmodel == null) {
         dispatcher.m_110912_(state, matrixStack, buffer, light, overlay);
      } else {
         dispatcher.m_110937_()
            .renderModel(
               matrixStack.m_85850_(),
               buffer.m_6299_(ItemBlockRenderTypes.m_109284_(state, false)),
               state,
               ibakedmodel,
               1.0F,
               1.0F,
               1.0F,
               light,
               overlay,
               ModelData.EMPTY,
               ItemBlockRenderTypes.m_109284_(state, false)
            );
      }
   }

   private boolean overrideModel() {
      ItemStack held = Minecraft.m_91087_().f_91074_.m_21205_();
      return held == null
         ? false
         : held.m_41720_() == CustomItems.wand || held.m_41720_() == CustomItems.scripter || held.m_41720_() == CustomBlocks.scripted_door_item;
   }
}
