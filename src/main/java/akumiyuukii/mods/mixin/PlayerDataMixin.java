package akumiyuukii.mods.mixin;

import akumiyuukii.mods.PlayerStats;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerDataMixin {

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void onReadData(CompoundTag compound, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        PlayerStats.loadFromNbt(player.getUUID(), compound);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void onWriteData(CompoundTag compound, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        CompoundTag statsTag = PlayerStats.saveToNbt(player.getUUID());
        if (statsTag != null && !statsTag.isEmpty()) {
            compound.put("AkumiYuukiiStats", statsTag.get("AkumiYuukiiStats"));
        }
    }

}