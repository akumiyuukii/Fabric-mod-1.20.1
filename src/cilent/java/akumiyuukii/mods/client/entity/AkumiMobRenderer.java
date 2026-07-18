package akumiyuukii.mods.client.entity;

import akumiyuukii.mods.AkumiYuukiiMods;
import akumiyuukii.mods.entity.AkumiMobEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders every custom mob with the vanilla humanoid (player/zombie-style) model. The texture is
 * chosen from the entity type's registered id, so dropping a PNG named "<mob_id>.png" into
 * assets/akumiyuukiimods/textures/entity/ is all that's needed to skin a mob.
 *
 * A fallback texture (vanilla steve) is used until you add your own, so a missing PNG never crashes
 * the client — it just shows the default skin.
 */
public class AkumiMobRenderer extends MobRenderer<AkumiMobEntity, HumanoidModel<AkumiMobEntity>> {

    private static final ResourceLocation FALLBACK =
            new ResourceLocation("minecraft", "textures/entity/steve.png");

    public AkumiMobRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(AkumiMobEntity entity) {
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (typeId != null && AkumiYuukiiMods.MOD_ID.equals(typeId.getNamespace())) {
            return new ResourceLocation(AkumiYuukiiMods.MOD_ID,
                    "textures/entity/" + typeId.getPath() + ".png");
        }
        return FALLBACK;
    }
}
