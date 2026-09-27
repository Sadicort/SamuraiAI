package noppes.npcs.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import noppes.npcs.blocks.tiles.TileMailbox;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiOpen;

public class BlockMailbox extends BlockInterface {
   public static final IntegerProperty ROTATION = IntegerProperty.m_61631_("rotation", 0, 3);
   public final int type;

   public BlockMailbox(int type) {
      super(Properties.m_60926_(Blocks.f_50075_).m_60918_(SoundType.f_56743_).m_60913_(5.0F, 10.0F));
      this.type = type;
   }

   public String m_7705_() {
      return "block.customnpcs.npcmailbox";
   }

   public VoxelShape m_7952_(BlockState p_196247_1_, BlockGetter p_196247_2_, BlockPos p_196247_3_) {
      return Shapes.m_83040_();
   }

   public boolean m_7357_(BlockState p_196266_1_, BlockGetter p_196266_2_, BlockPos p_196266_3_, PathComputationType p_196266_4_) {
      return false;
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult ray) {
      if (!level.f_46443_) {
         Packets.send((ServerPlayer)player, new PacketGuiOpen(EnumGuiType.PlayerMailbox, pos));
      }

      return InteractionResult.SUCCESS;
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{ROTATION});
   }

   public BlockState m_5573_(BlockPlaceContext context) {
      int l = Mth.m_14107_(context.m_43723_().m_146908_() * 4.0F / 360.0F + 0.5) & 3;
      return (BlockState)this.m_49966_().m_61124_(ROTATION, l % 4);
   }

   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new TileMailbox(pos, state).setModel(this.type);
   }
}
