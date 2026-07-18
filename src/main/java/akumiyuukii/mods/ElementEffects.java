package akumiyuukii.mods;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Signature on-hit procs for each element.
 *  - preDamageProc:  returns a damage multiplier applied to this hit (wind/quantum/physical).
 *  - postDamageProc: applies status / area effects after a confirmed hit (ice/fire/lightning)
 *    and spawns element-flavoured particles + sounds so every element is visibly alive.
 */
public final class ElementEffects {
    private ElementEffects() {}

    // Guards against lightning chain hits re-triggering more chains.
    public static boolean isChaining = false;

    public static double preDamageProc(Element element, RandomSource rand) {
        return switch (element) {
            // Physical: armor penetration approximated as a flat +15% on the hit.
            case PHYSICAL -> 1.15;
            // Wind: 25% chance to double damage (wind cut).
            case WIND -> rand.nextFloat() < 0.25f ? 2.0 : 1.0;
            // Quantum: 20% chance for a quantum crit (x2).
            case QUANTUM -> rand.nextFloat() < 0.20f ? 2.0 : 1.0;
            default -> 1.0;
        };
    }

    public static void postDamageProc(Element element, Player player, LivingEntity target,
                                      float damageDealt, RandomSource rand) {
        switch (element) {
            case PHYSICAL -> {
                // Armor pen already applied pre-damage; show a crit-style spark for feedback.
                spawnParticles(target, ParticleTypes.CRIT, 8);
                playSound(target, SoundEvents.PLAYER_ATTACK_CRIT);
            }
            case ICE -> {
                // 30% chance to freeze: strong slowness + dig-slowdown to "hold" the target.
                if (rand.nextFloat() < 0.30f) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4));
                    target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 2));
                    target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 60, 300));
                    spawnParticles(target, ParticleTypes.SNOWFLAKE, 20);
                    playSound(target, SoundEvents.PLAYER_HURT_FREEZE);
                }
            }
            case FIRE -> {
                // 30% chance to ignite (burn / DoT).
                if (rand.nextFloat() < 0.30f) {
                    target.setSecondsOnFire(4);
                    spawnParticles(target, ParticleTypes.FLAME, 16);
                    playSound(target, SoundEvents.FIRECHARGE_USE);
                }
            }
            case WIND -> {
                // Wind cut visual (double-damage roll happened pre-damage).
                spawnParticles(target, ParticleTypes.SWEEP_ATTACK, 4);
                playSound(target, SoundEvents.PLAYER_ATTACK_SWEEP);
            }
            case QUANTUM -> {
                // Quantum entanglement visual (x2 roll happened pre-damage).
                spawnParticles(target, ParticleTypes.PORTAL, 24);
                spawnParticles(target, ParticleTypes.WITCH, 8);
                playSound(target, SoundEvents.AMETHYST_BLOCK_CHIME);
            }
            case LIGHTNING -> {
                // 25% chance to chain to nearby enemies for a fraction of the damage.
                if (!isChaining && rand.nextFloat() < 0.25f && damageDealt > 0) {
                    isChaining = true;
                    try {
                        float chainDamage = damageDealt * 0.5f;
                        AABB area = target.getBoundingBox().inflate(4.0);
                        for (LivingEntity nearby : target.level().getEntitiesOfClass(LivingEntity.class, area)) {
                            if (nearby == target || nearby == player) continue;
                            if (!nearby.isAlive()) continue;
                            nearby.hurt(player.damageSources().playerAttack(player), chainDamage);
                            spawnParticles(nearby, ParticleTypes.ELECTRIC_SPARK, 12);
                        }
                        spawnParticles(target, ParticleTypes.ELECTRIC_SPARK, 20);
                        playSound(target, SoundEvents.LIGHTNING_BOLT_IMPACT);
                    } finally {
                        isChaining = false;
                    }
                }
            }
        }
    }

    /** Spawns particles centred on the target's body; server-side only (no-op on client). */
    private static void spawnParticles(LivingEntity target, net.minecraft.core.particles.SimpleParticleType type, int count) {
        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(type,
                    target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                    count, 0.4, 0.5, 0.4, 0.05);
        }
    }

    private static void playSound(LivingEntity target, net.minecraft.sounds.SoundEvent sound) {
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                sound, SoundSource.PLAYERS, 0.7f, 1.0f);
    }
}
