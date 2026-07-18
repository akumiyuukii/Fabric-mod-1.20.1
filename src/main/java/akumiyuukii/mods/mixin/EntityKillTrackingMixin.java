package akumiyuukii.mods.mixin;

import akumiyuukii.mods.PlayerFragments;
import akumiyuukii.mods.PlayerStats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class EntityKillTrackingMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void onDeath(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide) return;

        Entity attacker = damageSource.getEntity();
        if (attacker instanceof Player player) {
            PlayerStats stats = PlayerStats.get(player);

            int pts = 0;
            if (entity instanceof Player) pts = 1;
            else if (entity instanceof EnderDragon) pts = 25;
            else if (entity instanceof Warden) pts = 10;
            else pts = 1;

            stats.addPoints(pts);
        }
    }
}
