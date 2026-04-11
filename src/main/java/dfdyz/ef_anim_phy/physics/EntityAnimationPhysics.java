package dfdyz.ef_anim_phy.physics;

import com.mojang.blaze3d.vertex.PoseStack;
import dfdyz.ef_anim_phy.physics.bodies.DynamicBoneCollider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class EntityAnimationPhysics extends AnimationPhysics{

    public final LivingEntityPatch<?> entityPatch;

    protected float prevRotX = 0;
    protected float prevRotY = 0;
    protected float prevBodyY = 0;

    public EntityAnimationPhysics(LivingEntityPatch<?> entityPatch) {
        this.entityPatch = entityPatch;
    }

    public void tick(boolean useSubStep){
        if(!warmuped) {
            prevRotX = entityPatch.getOriginal().xRotO;
            prevRotY = entityPatch.getOriginal().yHeadRotO;
            prevBodyY = entityPatch.getYRotO();
        }
        var origin_RX = entityPatch.getOriginal().xRotO;
        var origin_RY = entityPatch.getOriginal().yHeadRotO;
        var origin_BY = entityPatch.getYRotO();

        entityPatch.getOriginal().xRotO = prevRotX;
        entityPatch.getOriginal().yHeadRotO = prevRotY;

        if(entityPatch instanceof AbstractClientPlayerPatch<?>)
            entityPatch.setYRotO(prevBodyY);

        var d = entityPatch.getOriginal().getDeltaMovement().scale(1f);
        if(useSubStep)
            this.tick((float) d.x, (float) d.y, (float) d.z, entityPatch.getYRotO(), entityPatch.getYRot()
                    ,entityPatch.getArmature(), entityPatch.getAnimator());
        else
            this.tick((float) d.x, (float) d.y, (float) d.z,entityPatch.getYRot()
                    ,entityPatch.getArmature(), entityPatch.getAnimator().getPose(1));

        prevRotX = origin_RX;
        prevRotY = origin_RY;
        prevBodyY = origin_BY;

        entityPatch.getOriginal().xRotO = origin_RX;
        entityPatch.getOriginal().yHeadRotO = origin_RY;
        if(entityPatch instanceof AbstractClientPlayerPatch<?>)
            entityPatch.setYRotO(origin_BY);
    }

    public void updateSBCCache(){
        staticBones.forEach((sbc, v) -> {
            sbc.updatePoseCache();
        });
    }

    public void renderDebug(LivingEntity entity, PoseStack poseStack, MultiBufferSource bufferSource, float pt){
        var ep = entity.getPosition(pt);
        staticBones.forEach((sbc, j) -> {
            sbc.renderDebug(poseStack, ep, bufferSource, pt);
        });
        dynamicChains.forEach((c) -> {
            c.renderDebug(poseStack, ep, bufferSource, pt);
        });
    }

    public void feedbackPose(Armature armature, Pose pose, float pt){
        float yRotLerp = Mth.rotLerp(pt, entityPatch.getYRotO(), entityPatch.getYRot());
        feedbackPose(yRotLerp, armature, pose, pt);
    }
}
