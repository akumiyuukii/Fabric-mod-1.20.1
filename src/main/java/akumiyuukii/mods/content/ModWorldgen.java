package akumiyuukii.mods.content;

import akumiyuukii.mods.AkumiYuukiiMods;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Adds every ore's placed feature to overworld biomes via Fabric's biome modification API.
 * The placed/configured feature JSON is generated under data/akumiyuukiimods/worldgen/.
 */
public final class ModWorldgen {
    private ModWorldgen() {}

    public static void register() {
        for (ModOres.OreDef def : ModOres.ORES) {
            ResourceKey<PlacedFeature> key = ResourceKey.create(
                    net.minecraft.core.registries.Registries.PLACED_FEATURE,
                    new ResourceLocation(AkumiYuukiiMods.MOD_ID, def.id()));
            BiomeModifications.addFeature(
                    BiomeSelectors.foundInOverworld(),
                    GenerationStep.Decoration.UNDERGROUND_ORES,
                    key);
        }
        AkumiYuukiiMods.LOGGER.info("Registered ore worldgen for overworld biomes.");
    }
}
