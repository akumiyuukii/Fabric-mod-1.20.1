package akumiyuukii.mods.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A generic humanoid hostile mob. All 15 new mobs are instances of this class, differing only in
 * their attributes (set per-type in {@link ModEntities}) and their texture (chosen by the renderer
 * from the entity type). Using one class keeps the 15 mobs from being 15 copy-pasted files and
 * guarantees none of them can crash from a missing custom behaviour.
 */
public class AkumiMobEntity extends Monster {

    public AkumiMobEntity(EntityType<? extends AkumiMobEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Default humanoid-ish attributes; individual types scale HP/damage in {@link ModEntities}. */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.5)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    /** Standard hostile spawn rules (dark, on solid ground) — shared by every type. */
    public static boolean canSpawn(EntityType<? extends AkumiMobEntity> type,
                                   net.minecraft.world.level.ServerLevelAccessor level,
                                   net.minecraft.world.entity.MobSpawnType spawnType,
                                   net.minecraft.core.BlockPos pos,
                                   net.minecraft.util.RandomSource random) {
        return Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity) {
            // Small lifesteal so the mobs feel a bit tougher than a vanilla zombie.
            this.heal(1.0f);
        }
        return hurt;
    }
}
