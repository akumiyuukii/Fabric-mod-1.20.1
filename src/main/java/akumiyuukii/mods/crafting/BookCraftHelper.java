package akumiyuukii.mods.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.Map;

/** Small shared helpers for reading enchantments off books/items during custom crafting. */
public final class BookCraftHelper {
    private BookCraftHelper() {}

    /** Level of the given enchantment stored on this stack (works for enchanted books too), or 0. */
    public static int levelOf(ItemStack stack, Enchantment ench) {
        if (stack.isEmpty()) return 0;
        Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(stack);
        Integer lvl = map.get(ench);
        return lvl != null ? lvl : 0;
    }

    /** True if the stack is an enchanted book (or any item) carrying vanilla Sharpness. */
    public static boolean hasSharpness(ItemStack stack) {
        return levelOf(stack, Enchantments.SHARPNESS) > 0;
    }

    /** True if the stack is an iron sword. */
    public static boolean isIronSword(ItemStack stack) {
        return stack.is(Items.IRON_SWORD);
    }
}
