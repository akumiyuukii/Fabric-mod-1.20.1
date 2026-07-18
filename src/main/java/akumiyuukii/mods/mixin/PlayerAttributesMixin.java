package akumiyuukii.mods.mixin;

import akumiyuukii.mods.PlayerStats;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerAttributesMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (!player.level().isClientSide) {
            PlayerStats stats = PlayerStats.get(player);

            // Sync Max Health
            AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealthAttr != null) {
                double baseValue = maxHealthAttr.getBaseValue();
                double expectedMax = stats.getMaxHp();
                if (baseValue != expectedMax) {
                    maxHealthAttr.setBaseValue(expectedMax);
                }
            }

            // Sync Attack Damage
            AttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackAttr != null) {
                double expectedAttack = stats.getAttackDamage();
                if (attackAttr.getBaseValue() != expectedAttack) {
                    attackAttr.setBaseValue(expectedAttack);
                }
            }
        }
    }
}