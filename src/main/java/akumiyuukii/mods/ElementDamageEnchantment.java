package akumiyuukii.mods;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * "Tăng sát thương [nguyên tố]" — one enchantment per element.
 * Increases the damage dealt while the player's element matches this enchantment's element.
 *
 * Level 1-10: +10% per level (10% .. 100%).
 * Level 11+ : each extra level adds +5%.
 */
public class ElementDamageEnchantment extends Enchantment {

    public final Element element;

    public ElementDamageEnchantment(Element element) {
        super(Rarity.RARE, EnchantmentCategory.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
        this.element = element;
    }

    public static double getBonusPercent(int level) {
        if (level <= 0) return 0.0;
        if (level <= 10) {
            return level * 0.10;
        }
        return 1.0 + (level - 10) * 0.05;
    }

    private static String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(level);
        };
    }

    @Override
    public Component getFullname(int level) {
        return Component.literal("Tăng sát thương " + element.displayName + " " + roman(level));
    }

    @Override
    public int getMinCost(int level) {
        return 10 + (level - 1) * 8;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 20;
    }

    @Override
    public int getMaxLevel() {
        return 10;
    }

    @Override
    public boolean isTreasureOnly() {
        return false;
    }

    @Override
    public boolean isTradeable() {
        return true;
    }

    @Override
    public boolean isDiscoverable() {
        return true;
    }
}
