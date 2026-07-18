package akumiyuukii.mods.content;

import akumiyuukii.mods.AkumiYuukiiMods;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.util.valueprovider.UniformInt;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * All 11 new ores plus their dropped items.
 *
 * Each ore is a {@link DropExperienceBlock} (drops XP like vanilla ores). The block itself, when
 * mined, drops the matching "gem"/"ingot"/"raw" item (wired up via the generated loot table in
 * {@code ModDataGeneration} / the runtime loot fallback). Textures go in
 * assets/akumiyuukiimods/textures/block and .../textures/item respectively — folders are created
 * with a README so you can drop your own art in.
 */
public final class ModOres {
    private ModOres() {}

    /** An ore definition: id, the dropped item's id, and how much XP the ore drops. */
    public record OreDef(String id, String dropId, int minXp, int maxXp, float hardness, boolean deepslateTint) {}

    // 10 normal ores + 1 endgame ore. dropId is the item you get from mining.
    public static final OreDef[] ORES = {
            new OreDef("tin_ore",      "tin_ingot",      0, 0, 3.0f, false),
            new OreDef("silver_ore",   "silver_ingot",   0, 0, 3.0f, false),
            new OreDef("ruby_ore",     "ruby",           3, 7, 3.5f, false),
            new OreDef("sapphire_ore", "sapphire",       3, 7, 3.5f, false),
            new OreDef("topaz_ore",    "topaz",          3, 7, 3.5f, false),
            new OreDef("onyx_ore",     "onyx",           3, 7, 3.5f, false),
            new OreDef("jade_ore",     "jade",           2, 5, 3.5f, false),
            new OreDef("cobalt_ore",   "cobalt_ingot",   0, 0, 4.0f, false),
            new OreDef("platinum_ore", "platinum_ingot", 0, 0, 4.0f, false),
            new OreDef("bismuth_ore",  "bismuth_ingot",  1, 3, 3.5f, false),
            new OreDef("rare_endgame_ore", "rare_endgame_gem", 8, 14, 6.0f, true),
    };

    public static final Map<String, Block> ORE_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, Item> DROP_ITEMS = new LinkedHashMap<>();
    /** ore id -> drop item id, used by the loot fallback. */
    public static final Map<String, String> ORE_TO_DROP = new LinkedHashMap<>();

    public static void register() {
        for (OreDef def : ORES) {
            // Dropped item (registered once even if shared; here each ore has a unique drop).
            Item drop = DROP_ITEMS.computeIfAbsent(def.dropId(), id -> Registry.register(
                    BuiltInRegistries.ITEM,
                    new ResourceLocation(AkumiYuukiiMods.MOD_ID, id),
                    new Item(new Item.Properties())
            ));

            // Ore block.
            BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
                    .mapColor(def.deepslateTint() ? MapColor.DEEPSLATE : MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(def.hardness(), 3.0f);

            UniformInt xp = (def.maxXp() > 0) ? UniformInt.of(def.minXp(), def.maxXp()) : UniformInt.of(0, 0);
            Block ore = Registry.register(
                    BuiltInRegistries.BLOCK,
                    new ResourceLocation(AkumiYuukiiMods.MOD_ID, def.id()),
                    new DropExperienceBlock(props, xp)
            );
            Registry.register(
                    BuiltInRegistries.ITEM,
                    new ResourceLocation(AkumiYuukiiMods.MOD_ID, def.id()),
                    new BlockItem(ore, new Item.Properties())
            );

            ORE_BLOCKS.put(def.id(), ore);
            ORE_TO_DROP.put(def.id(), def.dropId());
            // keep 'drop' referenced
            if (drop == null) throw new IllegalStateException("drop item null for " + def.dropId());
        }
        AkumiYuukiiMods.LOGGER.info("Registered {} ores and {} drop items.", ORE_BLOCKS.size(), DROP_ITEMS.size());
    }

    public static Item drop(String dropId) {
        return DROP_ITEMS.get(dropId);
    }
}
