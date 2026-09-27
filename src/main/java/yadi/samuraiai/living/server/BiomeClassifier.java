package yadi.samuraiai.living.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.Tags;
import yadi.samuraiai.living.world.engine.WorldPorts;
import yadi.samuraiai.living.world.regions.RegionType;
import yadi.samuraiai.world.ServerWorlds;

/**
 * Reads what kind of land a region is from the Minecraft map: the biome at its centre (from the generator, so no chunk is
 * loaded or generated for it) and, when the chunk happens to be loaded, the real surface height; otherwise the height is the
 * typical one for that kind of land. A region is classified once, when it is created; the World Engine keeps the result.
 */
final class BiomeClassifier implements WorldPorts.RegionClassifier {
    @Override public Classification classify(String dimension, double x, double z) {
        ServerLevel level = ServerWorlds.level(dimension).orElse(null);
        if (level == null) return new Classification(RegionType.FIELDS, "", 64);
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        int seaLevel = level.getSeaLevel();
        Holder<Biome> biome = level.getUncachedNoiseBiome(QuartPos.fromBlock(bx), QuartPos.fromBlock(seaLevel), QuartPos.fromBlock(bz));
        String name = biome.unwrapKey().map(k -> k.location().toString()).orElse("");
        RegionType type = typeOf(biome, name);
        double altitude;
        BlockPos pos = new BlockPos(bx, seaLevel, bz);
        if (level.isLoaded(pos)) altitude = level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz);
        else altitude = switch (type) { case MOUNTAINS -> seaLevel + 60; case COAST, RIVER, SWAMP -> seaLevel; default -> seaLevel + 4; };
        return new Classification(type, name, altitude);
    }

    static RegionType typeOf(Holder<Biome> biome, String name) {
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH)) return RegionType.COAST;
        if (biome.is(BiomeTags.IS_RIVER)) return RegionType.RIVER;
        if (biome.is(Tags.Biomes.IS_SWAMP) || name.contains("swamp")) return RegionType.SWAMP;
        if (biome.is(BiomeTags.IS_MOUNTAIN) || biome.is(BiomeTags.IS_HILL) || name.contains("peaks") || name.contains("slopes")) return RegionType.MOUNTAINS;
        if (biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA) || biome.is(BiomeTags.IS_JUNGLE) || name.contains("grove") || name.contains("bamboo")) return RegionType.FOREST;
        return RegionType.FIELDS;
    }
}
