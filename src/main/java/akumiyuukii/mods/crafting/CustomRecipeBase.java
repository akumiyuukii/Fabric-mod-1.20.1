package akumiyuukii.mods.crafting;

import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;

/**
 * Thin base for our special crafting recipes. In 1.20.1 {@link CustomRecipe} takes only a
 * {@link CraftingBookCategory} (recipe IDs are assigned by the recipe manager), so this just fixes
 * the category to MISC for all of our book recipes.
 */
public abstract class CustomRecipeBase extends CustomRecipe {
    protected CustomRecipeBase(CraftingBookCategory category) {
        super(category);
    }
}
