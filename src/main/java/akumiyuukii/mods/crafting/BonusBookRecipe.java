package akumiyuukii.mods.crafting;

import akumiyuukii.mods.AkumiYuukiiMods;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;

/**
 * Crafting-table recipe for the "Tăng sát thương kèm theo" book. Two ways to craft, both handled
 * here (computed in code because the output is an NBT enchanted book, which vanilla JSON recipes
 * cannot produce):
 *
 *  1) A book/item carrying Sharpness  +  an iron sword           -> bonus book level I.
 *  2) Two bonus books of the SAME level N (N < 10)               -> one bonus book level N+1.
 *
 * Nothing else in the grid is allowed, so it never fires by accident.
 */
public class BonusBookRecipe extends CustomRecipeBase {

    public BonusBookRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(net.minecraft.world.inventory.CraftingContainer inv, Level level) {
        return computeResult(inv) != null;
    }

    @Override
    public ItemStack assemble(net.minecraft.world.inventory.CraftingContainer inv, RegistryAccess registryAccess) {
        ItemStack result = computeResult(inv);
        return result != null ? result : ItemStack.EMPTY;
    }

    /** Returns the crafted book, or null if the grid isn't a valid combination. */
    private ItemStack computeResult(net.minecraft.world.inventory.CraftingContainer inv) {
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) items.add(s);
        }
        if (items.size() != 2) return null;

        ItemStack a = items.get(0);
        ItemStack b = items.get(1);

        // Case 2: two bonus books of the same level -> next level.
        int la = BookCraftHelper.levelOf(a, AkumiYuukiiMods.BONUS_DAMAGE_ENCHANTMENT);
        int lb = BookCraftHelper.levelOf(b, AkumiYuukiiMods.BONUS_DAMAGE_ENCHANTMENT);
        if (la > 0 && lb > 0 && la == lb && la < AkumiYuukiiMods.BONUS_DAMAGE_ENCHANTMENT.getMaxLevel()) {
            return makeBook(la + 1);
        }

        // Case 1: sharpness book + iron sword -> level I. (Neither may already be a bonus book.)
        if (la == 0 && lb == 0) {
            boolean aSharp = BookCraftHelper.hasSharpness(a) && a.is(Items.ENCHANTED_BOOK);
            boolean bSharp = BookCraftHelper.hasSharpness(b) && b.is(Items.ENCHANTED_BOOK);
            boolean aSword = BookCraftHelper.isIronSword(a);
            boolean bSword = BookCraftHelper.isIronSword(b);
            if ((aSharp && bSword) || (bSharp && aSword)) {
                return makeBook(1);
            }
        }
        return null;
    }

    private static ItemStack makeBook(int level) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantedBookItem.addEnchantment(book,
                new EnchantmentInstance(AkumiYuukiiMods.BONUS_DAMAGE_ENCHANTMENT, level));
        return book;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.BONUS_BOOK_SERIALIZER;
    }
}
