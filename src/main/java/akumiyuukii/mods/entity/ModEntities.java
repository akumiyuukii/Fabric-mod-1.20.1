package akumiyuukii.mods.entity;

import akumiyuukii.mods.AkumiYuukiiMods;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registers all 15 custom mobs.
 *
 * There are two groups:
 *  - 5 "named" bosses/elites: Astralite, Eternium, Chronite, Titanite, Draconium (tougher).
 *  - 10 "ore" mobs, one themed to each of the 10 non-endgame ores (tin..bismuth).
 *
 * Every type shares {@link AkumiMobEntity}; only the attribute numbers differ. Textures are
 * expected at assets/akumiyuukiimods/textures/entity/<id>.png (the folder is created with a
 * README so you can drop your own textures in).
 */
public final class ModEntities {
    private ModEntities() {}

    /** id -> its registered EntityType, in registration order (used by the client renderer too). */
    public static final Map<String, EntityType<AkumiMobEntity>> TYPES = new LinkedHashMap<>();

    /** A mob definition: display name + base stats. */
    public record MobDef(String id, String displayName, double hp, double damage, double speed,
                         float width, float height) {}

    // The 5 named elites — stronger, meant as mini-bosses.
    public static final MobDef[] NAMED = {
            new MobDef("astralite", "Astralite", 60, 8, 0.28, 0.7f, 2.1f),
            new MobDef("eternium",  "Eternium",  80, 10, 0.26, 0.8f, 2.3f),
            new MobDef("chronite",  "Chronite",  70, 9, 0.30, 0.6f, 1.9f),
            new MobDef("titanite",  "Titanite",  120, 12, 0.22, 0.9f, 2.5f),
            new MobDef("draconium", "Draconium", 150, 16, 0.30, 1.0f, 2.6f),
    };

    // The 10 ore-themed mobs — ordinary hostiles that roam near their ore's depths.
    public static final MobDef[] ORE_MOBS = {
            new MobDef("tin_guardian",      "Tin Guardian",      24, 4, 0.25, 0.6f, 1.95f),
            new MobDef("silver_guardian",   "Silver Guardian",   28, 5, 0.26, 0.6f, 1.95f),
            new MobDef("ruby_guardian",     "Ruby Guardian",     34, 6, 0.25, 0.6f, 1.95f),
            new MobDef("sapphire_guardian", "Sapphire Guardian", 34, 6, 0.26, 0.6f, 1.95f),
            new MobDef("topaz_guardian",    "Topaz Guardian",    32, 6, 0.27, 0.6f, 1.95f),
            new MobDef("onyx_guardian",     "Onyx Guardian",     40, 7, 0.24, 0.6f, 1.95f),
            new MobDef("jade_guardian",     "Jade Guardian",     36, 6, 0.26, 0.6f, 1.95f),
            new MobDef("cobalt_guardian",   "Cobalt Guardian",   44, 7, 0.25, 0.6f, 1.95f),
            new MobDef("platinum_guardian", "Platinum Guardian", 50, 8, 0.24, 0.6f, 1.95f),
            new MobDef("bismuth_guardian",  "Bismuth Guardian",  46, 8, 0.25, 0.6f, 1.95f),
    };

    public static void register() {
        for (MobDef def : NAMED) registerOne(def);
        for (MobDef def : ORE_MOBS) registerOne(def);
        AkumiYuukiiMods.LOGGER.info("Registered {} custom mobs.", TYPES.size());
    }

    private static void registerOne(MobDef def) {
        EntityType<AkumiMobEntity> type = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                new ResourceLocation(AkumiYuukiiMods.MOD_ID, def.id()),
                EntityType.Builder.of(AkumiMobEntity::new, MobCategory.MONSTER)
                        .sized(def.width(), def.height())
                        .clientTrackingRange(10)
                        .build(def.id())
        );
        TYPES.put(def.id(), type);
        FabricDefaultAttributeRegistry.register(type, AkumiMobEntity.createAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, def.hp())
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, def.damage())
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, def.speed())
                .build());
    }
}
