package akumiyuukii.mods.mixin;

import akumiyuukii.mods.AkumiYuukiiMods;
import akumiyuukii.mods.BonusDamageConfig;
import akumiyuukii.mods.DamageCounter;
import akumiyuukii.mods.Element;
import akumiyuukii.mods.ElementDamageEnchantment;
import akumiyuukii.mods.ElementEffects;
import akumiyuukii.mods.PlayerStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityDamageMixin {

    // Target's health + absorption captured just before the hit is processed.
    @Unique
    private float akumiyuukii$healthBeforeHit;

    // The attacking player + their element for this hit, captured at HEAD for post-damage procs.
    @Unique
    private Player akumiyuukii$attacker;
    @Unique
    private Element akumiyuukii$attackerElement;

    /**
     * Applies the "Tăng sát thương kèm theo" bonus, folded into the SAME hit so it isn't
     * swallowed by invulnerability frames.
     *
     * Two parts:
     *  - Stack multiplier: while the attacker has active stacks, EVERY damage source they deal
     *    is multiplied by (1 + stacks * perStack). This is "local" to the player, not tied to
     *    the weapon still being held.
     *  - Base enchant bonus: only when landing a melee attack with the enchanted weapon, add a
     *    stack (refreshing the 5-minute timer) and add the flat weapon%/stat% bonus damage.
     */
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float akumiyuukii$addBonusDamage(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) return amount;
        if (amount <= 0) return amount;

        if (!(source.getEntity() instanceof Player player)) return amount;

        long gameTime = player.level().getGameTime();
        PlayerStats stats = PlayerStats.get(player);
        Element element = stats.getElement();

        // Remember for the post-damage proc (ice freeze / fire burn / lightning chain).
        akumiyuukii$attacker = player;
        akumiyuukii$attackerElement = element;

        double bonusFlat = 0.0;

        // Melee attack with the bonus-damage enchanted weapon: grow a stack + add the base flat bonus.
        if (source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) {
            ItemStack weapon = player.getMainHandItem();
            int level = weapon.isEmpty() ? 0 : EnchantmentHelper.getItemEnchantmentLevel(
                    AkumiYuukiiMods.BONUS_DAMAGE_ENCHANTMENT, weapon);
            if (level > 0) {
                int maxStacks = BonusDamageConfig.getMaxStacks(level);
                int stacks = stats.addBonusStack(maxStacks, level, gameTime);

                double weaponDamage = akumiyuukii$getWeaponAttackDamage(weapon);
                double statDamage = stats.getAttackDamage();
                double weaponPercent = BonusDamageConfig.getWeaponDamagePercent(level);
                double statPercent = BonusDamageConfig.getStatDamagePercent(level);

                bonusFlat = (weaponDamage * weaponPercent) + (statDamage * statPercent);

                if (player instanceof ServerPlayer serverPlayer) {
                    akumiyuukii.mods.BonusStackSync.send(serverPlayer, stacks, maxStacks, level,
                            weaponPercent, statPercent, BonusDamageConfig.getPerStackBonus(level));
                }
            }
        }

        // Stack multiplier applies to ALL of this player's damage sources.
        double multiplier = stats.getBonusStackMultiplier(gameTime);

        // Element damage %: the element-matching enchant on the held weapon boosts this hit,
        // but only while the player's chosen element matches that enchant (HSR-style).
        double elementMultiplier = 1.0;
        ItemStack weapon = player.getMainHandItem();
        if (!weapon.isEmpty()) {
            ElementDamageEnchantment matchEnch = AkumiYuukiiMods.ELEMENT_ENCHANTMENTS.get(element);
            if (matchEnch != null) {
                int elvl = EnchantmentHelper.getItemEnchantmentLevel(matchEnch, weapon);
                if (elvl > 0) {
                    elementMultiplier += ElementDamageEnchantment.getBonusPercent(elvl);
                }
            }
        }

        // Element pre-damage proc (wind cut / quantum crit / physical pen).
        double procMultiplier = ElementEffects.preDamageProc(element, player.getRandom());

        double total = (amount + bonusFlat) * multiplier * elementMultiplier * procMultiplier;
        return (float) Math.max(0.0, total);
    }

    @Inject(method = "hurt", at = @At("HEAD"))
    private void captureHealthBeforeHit(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        akumiyuukii$healthBeforeHit = entity.getHealth() + entity.getAbsorptionAmount();
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Player attacker = akumiyuukii$attacker;
        Element element = akumiyuukii$attackerElement;
        akumiyuukii$attacker = null;
        akumiyuukii$attackerElement = null;

        if (!cir.getReturnValue()) return;

        if (source.getEntity() instanceof ServerPlayer player) {
            LivingEntity entity = (LivingEntity) (Object) this;
            float healthAfter = entity.getHealth() + entity.getAbsorptionAmount();
            // Actual damage dealt = health/absorption lost this hit.
            float actualDamage = akumiyuukii$healthBeforeHit - healthAfter;
            if (actualDamage > 0) {
                Element e = element != null ? element : PlayerStats.get(player).getElement();
                DamageCounter.sendDamagePacket(player, actualDamage, e.ordinal());

                // Element post-damage proc (ice/fire/lightning).
                if (attacker != null) {
                    ElementEffects.postDamageProc(e, attacker, entity, actualDamage, entity.getRandom());
                }
            }
        }
    }

    /**
     * The sword/tool's own attack damage (sum of its additive ATTACK_DAMAGE modifiers in the
     * main hand), independent of the player's stat-based base damage.
     */
    @Unique
    private double akumiyuukii$getWeaponAttackDamage(ItemStack weapon) {
        double damage = 0.0;
        var modifiers = weapon.getAttributeModifiers(EquipmentSlot.MAINHAND)
                .get(Attributes.ATTACK_DAMAGE);
        for (AttributeModifier modifier : modifiers) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                damage += modifier.getAmount();
            }
        }
        return damage;
    }
}
