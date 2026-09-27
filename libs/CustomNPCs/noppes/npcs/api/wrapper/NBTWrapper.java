package noppes.npcs.api.wrapper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.INbt;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.util.NBTJsonUtil;

public class NBTWrapper implements INbt {
   private CompoundTag compound;

   public NBTWrapper(CompoundTag compound) {
      this.compound = compound;
   }

   @Override
   public void remove(String key) {
      this.compound.m_128473_(key);
   }

   @Override
   public boolean has(String key) {
      return this.compound.m_128441_(key);
   }

   @Override
   public boolean getBoolean(String key) {
      return this.compound.m_128471_(key);
   }

   @Override
   public void setBoolean(String key, boolean value) {
      this.compound.m_128379_(key, value);
   }

   @Override
   public short getShort(String key) {
      return this.compound.m_128448_(key);
   }

   @Override
   public void setShort(String key, short value) {
      this.compound.m_128376_(key, value);
   }

   @Override
   public int getInteger(String key) {
      return this.compound.m_128451_(key);
   }

   @Override
   public void setInteger(String key, int value) {
      this.compound.m_128405_(key, value);
   }

   @Override
   public byte getByte(String key) {
      return this.compound.m_128445_(key);
   }

   @Override
   public void setByte(String key, byte value) {
      this.compound.m_128344_(key, value);
   }

   @Override
   public long getLong(String key) {
      return this.compound.m_128454_(key);
   }

   @Override
   public void setLong(String key, long value) {
      this.compound.m_128356_(key, value);
   }

   @Override
   public double getDouble(String key) {
      return this.compound.m_128459_(key);
   }

   @Override
   public void setDouble(String key, double value) {
      this.compound.m_128347_(key, value);
   }

   @Override
   public float getFloat(String key) {
      return this.compound.m_128457_(key);
   }

   @Override
   public void setFloat(String key, float value) {
      this.compound.m_128350_(key, value);
   }

   @Override
   public String getString(String key) {
      return this.compound.m_128461_(key);
   }

   @Override
   public void putString(String key, String value) {
      this.compound.m_128359_(key, value);
   }

   @Override
   public byte[] getByteArray(String key) {
      return this.compound.m_128463_(key);
   }

   @Override
   public void setByteArray(String key, byte[] value) {
      this.compound.m_128382_(key, value);
   }

   @Override
   public int[] getIntegerArray(String key) {
      return this.compound.m_128465_(key);
   }

   @Override
   public void setIntegerArray(String key, int[] value) {
      this.compound.m_128385_(key, value);
   }

   @Override
   public Object[] getList(String key, int type) {
      ListTag list = this.compound.m_128437_(key, type);
      Object[] nbts = new Object[list.size()];

      for (int i = 0; i < list.size(); i++) {
         if (list.m_7264_() == 10) {
            nbts[i] = NpcAPI.Instance().getINbt(list.m_128728_(i));
         } else if (list.m_7264_() == 8) {
            nbts[i] = list.m_128778_(i);
         } else if (list.m_7264_() == 6) {
            nbts[i] = list.m_128772_(i);
         } else if (list.m_7264_() == 5) {
            nbts[i] = list.m_128775_(i);
         } else if (list.m_7264_() == 3) {
            nbts[i] = list.m_128763_(i);
         } else if (list.m_7264_() == 11) {
            nbts[i] = list.m_128767_(i);
         }
      }

      return nbts;
   }

   @Override
   public int getListType(String key) {
      Tag b = this.compound.m_128423_(key);
      if (b == null) {
         return 0;
      } else if (b.m_7060_() != 9) {
         throw new CustomNPCsException("NBT tag " + key + " isn't a list");
      } else {
         return ((ListTag)b).m_7264_();
      }
   }

   @Override
   public void setList(String key, Object[] value) {
      ListTag list = new ListTag();

      for (Object nbt : value) {
         if (nbt instanceof INbt) {
            list.add(((INbt)nbt).getMCNBT());
         } else if (nbt instanceof String) {
            list.add(StringTag.m_129297_((String)nbt));
         } else if (nbt instanceof Double) {
            list.add(DoubleTag.m_128500_((Double)nbt));
         } else if (nbt instanceof Float) {
            list.add(FloatTag.m_128566_((Float)nbt));
         } else if (nbt instanceof Integer) {
            list.add(IntTag.m_128679_((Integer)nbt));
         } else if (nbt instanceof int[]) {
            list.add(new IntArrayTag((int[])nbt));
         }
      }

      this.compound.m_128365_(key, list);
   }

   @Override
   public INbt getCompound(String key) {
      return NpcAPI.Instance().getINbt(this.compound.m_128469_(key));
   }

   @Override
   public void setCompound(String key, INbt value) {
      if (value == null) {
         throw new CustomNPCsException("Value cant be null");
      }

      this.compound.m_128365_(key, value.getMCNBT());
   }

   @Override
   public String[] getKeys() {
      return this.compound.m_128431_().toArray(new String[this.compound.m_128431_().size()]);
   }

   @Override
   public int getType(String key) {
      return this.compound.m_128423_(key).m_7060_();
   }

   @Override
   public CompoundTag getMCNBT() {
      return this.compound;
   }

   @Override
   public String toJsonString() {
      return NBTJsonUtil.Convert(this.compound);
   }

   @Override
   public boolean isEqual(INbt nbt) {
      return nbt == null ? false : this.compound.equals(nbt.getMCNBT());
   }

   @Override
   public void clear() {
      for (String name : this.compound.m_128431_()) {
         this.compound.m_128473_(name);
      }
   }

   @Override
   public boolean isEmpty() {
      return this.compound.m_128456_();
   }

   @Override
   public void merge(INbt nbt) {
      this.compound.m_128391_(nbt.getMCNBT());
   }

   @Override
   public void mcSetTag(String key, Tag base) {
      this.compound.m_128365_(key, base);
   }

   @Override
   public Tag mcGetTag(String key) {
      return this.compound.m_128423_(key);
   }
}
