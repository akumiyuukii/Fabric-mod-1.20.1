package akumiyuukii.mods.content;

import akumiyuukii.mods.AkumiYuukiiMods;
import akumiyuukii.mods.entity.ModEntities;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Creative tabs for the new content:
 *  - "Quặng & Vật phẩm": every ore block + its dropped gems/ingots.
 *  - "Trứng quái": a spawn egg for each of the 15 mobs (so they can be spawned without commands).
 *
 * Spawn eggs are registered here (not in ModEntities) because they depend on the entity types
 * already existing; call {@link #register()} after both ores and entities are registered.
 */
public final class ModItemGroups {
    private ModItemGroups() {}

    public static final Map<String, SpawnEggItem> SPAWN_EGGS = new LinkedHashMap<>();

    public static CreativeModeTab ORE_TAB;
    public static CreativeModeTab EGG_TAB;

    public static void register() {
        // Spawn eggs for every registered mob type.
        int i = 0;
        for (var entry : ModEntities.TYPES.entrySet()) {
            int primary = 0x333355 + (i * 0x111111);
            int secondary = 0xAAAAFF - (i * 0x080808);
            SpawnEggItem egg = new SpawnEggItem(entry.getValue(),
                    primary & 0xFFFFFF, secondary & 0xFFFFFF, new net.minecraft.world.item.Item.Properties());
            Registry.register(BuiltInRegistries.ITEM,
                    new ResourceLocation(AkumiYuukiiMods.MOD_ID, entry.getKey() + "_spawn_egg"), egg);
            SPAWN_EGGS.put(entry.getKey(), egg);
            i++;
        }

        ORE_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                new ResourceLocation(AkumiYuukiiMods.MOD_ID, "ores"),
                FabricItemGroup.builder()
                        .title(Component.translatable("itemGroup.akumiyuukiimods.ores"))
                        .icon(() -> {
                            Block first = ModOres.ORE_BLOCKS.get("rare_endgame_ore");
                            return first != null ? new ItemStack(first) : new ItemStack(Items.IRON_ORE);
                        })
                        .displayItems((params, output) -> {
                            for (Block ore : ModOres.ORE_BLOCKS.values()) output.accept(ore);
                            for (var item : ModOres.DROP_ITEMS.values()) output.accept(item);
                        })
                        .build());

        EGG_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                new ResourceLocation(AkumiYuukiiMods.MOD_ID, "spawn_eggs"),
                FabricItemGroup.builder()
                        .title(Component.translatable("itemGroup.akumiyuukiimods.spawn_eggs"))
                        .icon(() -> new ItemStack(Items.ZOMBIE_SPAWN_EGG))
                        .displayItems((params, output) -> {
                            for (SpawnEggItem egg : SPAWN_EGGS.values()) output.accept(egg);
                        })
                        .build());

        AkumiYuukiiMods.LOGGER.info("Registered {} spawn eggs + 2 creative tabs.", SPAWN_EGGS.size());
    }
}
