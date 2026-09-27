package noppes.npcs.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagType;
import net.minecraft.resources.ResourceLocation;

public class ValueUtil {
   public static final UUID EMPTY_UUID = new UUID(0L, 0L);

   public static float correctFloat(float given, float min, float max) {
      if (given < min) {
         return min;
      } else {
         return given > max ? max : given;
      }
   }

   public static int CorrectInt(int given, int min, int max) {
      if (given < min) {
         return min;
      } else {
         return given > max ? max : given;
      }
   }

   public static String nbtToJson(CompoundTag nbt) {
      return new Gson().toJson(getJsonValue(nbt));
   }

   private static JsonElement getJsonValue(Tag value) {
      if (value.m_6458_() == CompoundTag.f_128326_) {
         CompoundTag nbt = (CompoundTag)value;
         JsonObject root = new JsonObject();

         for (String key : nbt.m_128431_()) {
            Tag n = nbt.m_128423_(key);
            JsonElement ele = getJsonValue(n);
            if (ele != null) {
               JsonObject ob = new JsonObject();
               ob.addProperty("type", n.m_6458_().m_5987_());
               ob.addProperty("type_id", n.m_7060_());
               ob.addProperty("pretty_type", n.m_6458_().m_5986_());
               ob.add("value", ele);
               root.add(key, ob);
            }
         }

         return root;
      } else {
         if (value == StringTag.f_129288_) {
            return new JsonPrimitive(value.m_7916_());
         }

         if (value instanceof NumericTag) {
            return new JsonPrimitive(((NumericTag)value).m_8103_());
         }

         if (!(value instanceof CollectionTag)) {
            return null;
         }

         JsonArray jsonValue = new JsonArray();

         for (Tag n : (CollectionTag)value) {
            jsonValue.add(getJsonValue(n));
         }

         return jsonValue;
      }
   }

   public static CompoundTag jsonToNbt(String json) {
      JsonObject ob = (JsonObject)new Gson().fromJson(json, JsonObject.class);
      return toNbt(ob);
   }

   private static CompoundTag toNbt(JsonObject json) {
      CompoundTag nbt = new CompoundTag();

      for (Entry<String, JsonElement> entry : json.entrySet()) {
         String key = entry.getKey();
         JsonObject ele = (JsonObject)entry.getValue();
         TagType<? extends Tag> type = stringToType(ele.get("type").getAsString());
         if (type == StringTag.f_129288_) {
            nbt.m_128359_(key, ele.get("value").getAsString());
         }

         if (type == IntTag.f_128670_) {
            nbt.m_128405_(key, ele.get("value").getAsInt());
         }

         if (type == ByteTag.f_128255_) {
            nbt.m_128344_(key, ele.get("value").getAsByte());
         }

         if (type == LongTag.f_128873_) {
            nbt.m_128356_(key, ele.get("value").getAsLong());
         }

         if (type == FloatTag.f_128560_) {
            nbt.m_128350_(key, ele.get("value").getAsFloat());
         }

         if (type == DoubleTag.f_128494_) {
            nbt.m_128347_(key, ele.get("value").getAsDouble());
         }

         if (type == ShortTag.f_129244_) {
            nbt.m_128376_(key, ele.get("value").getAsShort());
         }

         if (type == CompoundTag.f_128326_) {
            nbt.m_128365_(key, toNbt((JsonObject)ele.get("value")));
         }

         if (type == IntArrayTag.f_128599_) {
            JsonArray array = (JsonArray)ele.get("value");
            nbt.m_128365_(key, new IntArrayTag(StreamSupport.stream(array.spliterator(), false).map(JsonElement::getAsInt).collect(Collectors.toList())));
         }

         if (type == ByteArrayTag.f_128185_) {
            JsonArray array = (JsonArray)ele.get("value");
            nbt.m_128365_(key, new ByteArrayTag(StreamSupport.stream(array.spliterator(), false).map(JsonElement::getAsByte).collect(Collectors.toList())));
         }

         if (type == LongArrayTag.f_128800_) {
            JsonArray array = (JsonArray)ele.get("value");
            nbt.m_128365_(key, new LongArrayTag(StreamSupport.stream(array.spliterator(), false).map(JsonElement::getAsLong).collect(Collectors.toList())));
         }
      }

      return nbt;
   }

   private static TagType<? extends Tag> stringToType(String type) {
      if (type.equals(IntTag.f_128670_.m_5987_())) {
         return IntTag.f_128670_;
      } else if (type.equals(ByteTag.f_128255_.m_5987_())) {
         return ByteTag.f_128255_;
      } else if (type.equals(FloatTag.f_128560_.m_5987_())) {
         return FloatTag.f_128560_;
      } else if (type.equals(LongTag.f_128873_.m_5987_())) {
         return LongTag.f_128873_;
      } else if (type.equals(DoubleTag.f_128494_.m_5987_())) {
         return DoubleTag.f_128494_;
      } else if (type.equals(ShortTag.f_129244_.m_5987_())) {
         return ShortTag.f_129244_;
      } else if (type.equals(CompoundTag.f_128326_.m_5987_())) {
         return CompoundTag.f_128326_;
      } else if (type.equals(IntArrayTag.f_128599_.m_5987_())) {
         return IntArrayTag.f_128599_;
      } else if (type.equals(ByteArrayTag.f_128185_.m_5987_())) {
         return ByteArrayTag.f_128185_;
      } else {
         return type.equals(LongArrayTag.f_128800_.m_5987_()) ? LongArrayTag.f_128800_ : StringTag.f_129288_;
      }
   }

   public static boolean isValidPath(String s) {
      for (int i = 0; i < s.length(); i++) {
         if (!ResourceLocation.m_135828_(s.charAt(i))) {
            return false;
         }
      }

      return true;
   }
}
