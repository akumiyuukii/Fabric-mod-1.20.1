package akumiyuukii.mods.mixin;

import akumiyuukii.mods.PlayerStats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
public class PlayerDamageMixin {

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float modifyIncomingDamage(float amount, DamageSource source) {
        Player player = (Player) (Object) this;
        if (!player.level().isClientSide && amount > 0) {
            PlayerStats stats = PlayerStats.get(player);
            double defense = stats.getDefense();
            if (defense > 0) {
                // Defense reduces damage: each defense level reduces by damage/5
                // damage = damage * (1 - defense/5)
                float reduction = Math.min((float) (defense / 5.0), 0.95f);
                float reducedDamage = amount * (1.0f - reduction);
                if (reducedDamage < 0.5f) {
                    reducedDamage = 0.5f;
                }
                return reducedDamage;
            }
        }
        return amount;
    }
}