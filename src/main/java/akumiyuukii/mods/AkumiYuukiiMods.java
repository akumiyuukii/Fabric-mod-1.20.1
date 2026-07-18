package akumiyuukii.mods;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

public class AkumiYuukiiMods implements ModInitializer {
    public static final String MOD_ID = "akumiyuukiimods";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static BonusDamageEnchantment BONUS_DAMAGE_ENCHANTMENT;

    // One "Tăng sát thương [nguyên tố]" enchantment per element.
    public static final Map<Element, ElementDamageEnchantment> ELEMENT_ENCHANTMENTS = new EnumMap<>(Element.class);

    public static CreativeModeTab BOOK_TAB;

    @Override
    public void onInitialize() {
        // Register the base bonus-damage enchantment.
        BONUS_DAMAGE_ENCHANTMENT = Registry.register(
                BuiltInRegistries.ENCHANTMENT,
                new ResourceLocation(MOD_ID, BonusDamageEnchantment.ENCHANTMENT_ID),
                new BonusDamageEnchantment()
        );

        // Register one element-damage enchantment per element.
        for (Element element : Element.values()) {
            ElementDamageEnchantment ench = Registry.register(
                    BuiltInRegistries.ENCHANTMENT,
                    new ResourceLocation(MOD_ID, "element_damage_" + element.id),
                    new ElementDamageEnchantment(element)
            );
            ELEMENT_ENCHANTMENTS.put(element, ench);
        }

        // Register a creative tab with ready-made enchanted books.
        BOOK_TAB = Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                new ResourceLocation(MOD_ID, "books"),
                FabricItemGroup.builder()
                        .title(Component.literal("AkumiYuukii - Sách phù phép"))
                        .icon(() -> new ItemStack(Items.ENCHANTED_BOOK))
                        .displayItems((params, output) -> {
                            // Bonus damage books I-V.
                            for (int level = 1; level <= 5; level++) {
                                output.accept(book(BONUS_DAMAGE_ENCHANTMENT, level));
                            }
                            // Element damage books I-X for each element.
                            for (Element element : Element.values()) {
                                ElementDamageEnchantment ench = ELEMENT_ENCHANTMENTS.get(element);
                                for (int level = 1; level <= 10; level++) {
                                    output.accept(book(ench, level));
                                }
                            }
                        })
                        .build()
        );

        // Server receiver for the player's element choice.
        ServerPlayNetworking.registerGlobalReceiver(ElementNetworking.SELECT_ID,
                (server, player, handler, buf, responseSender) -> {
                    int ordinal = buf.readInt();
                    server.execute(() -> {
                        PlayerStats stats = PlayerStats.get(player);
                        Element chosen = Element.byOrdinal(ordinal);
                        boolean firstTime = !stats.hasChosenElement();
                        if (firstTime) {
                            stats.setElement(chosen);
                            stats.markElementChosen();
                        } else {
                            // Re-choosing costs 100 stat points.
                            if (stats.getPoints() >= 100) {
                                stats.addPoints(-100);
                                stats.setElement(chosen);
                            }
                        }
                        ElementNetworking.sync(player, stats);
                    });
                });

        // On join, sync element state so the client can force the selection screen if needed.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            ElementNetworking.sync(player, PlayerStats.get(player));
        });

        LOGGER.info("AkumiYuukii Mods initialized! Enchantments and books registered.");
    }

    private static ItemStack book(net.minecraft.world.item.enchantment.Enchantment ench, int level) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(ench, level));
        return book;
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
