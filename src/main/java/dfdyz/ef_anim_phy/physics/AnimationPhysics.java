package dfdyz.ef_anim_phy.physics;

import com.google.common.collect.Maps;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import dfdyz.ef_anim_phy.physics.bodies.*;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.AnimationTransformEntry;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static yesman.epicfight.api.animation.JointTransform.*;

public class AnimationPhysics {

    //public final LivingEntityPatch<?> entityPatch;
    public final List<JointSpring> jointSprings = new ArrayList<>();
    public final PhysicsSpace physicsSpace = new PhysicsSpace(PhysicsSpace.BroadphaseType.AXIS_SWEEP_3);
    public final Map<StaticBoneCollider, String> staticBones = Maps.newHashMap();
    public final List<DynamicBoneChain> dynamicChains = new ArrayList<>();
    public final HashMap<String, DynamicBoneCollider> dynamicBones = new HashMap<>();

    public AnimationPhysics(){
        physicsSpace.setAccuracy(1 / 100f);
    }

    public void prepare(Armature armature){
        armature.setPose(new Pose());
    }

    public void addChain(DynamicBoneChain chain){
        dynamicChains.add(chain);
        for (int i = 0; i < chain.jointChain.length; i++) {
            var jtName = chain.jointChain[i];
            var dbc = chain.bodyChain[i];
            dynamicBones.put(jtName, dbc);
        }
    }


    public StaticBoneCollider addStaticBone(Joint joint, CollisionShape shape, int group){
        var sbc = new StaticBoneCollider(shape);
        staticBones.put(sbc, joint.getName());
        sbc.setGroup(group);
        sbc.init();
        physicsSpace.addCollisionObject(sbc.body);

        return sbc;
    }
    public StaticBoneCollider addStaticBone(Joint joint, CollisionShape shape, int group, int mask){
        var sbc = new StaticBoneCollider(shape);
        staticBones.put(sbc, joint.getName());
        sbc.setGroup(group);
        sbc.setMask(mask);
        sbc.init();
        physicsSpace.addCollisionObject(sbc.body);
        return sbc;
    }



    private final OpenMatrix4f COLLIDER_TRANSFORM = new OpenMatrix4f();



    public void updateStaticBonesStep(float dx, float dy, float dz, float yRotO, float yRot,
                                      Armature armature, Animator animator, float pt){
        float yRotLerp = Mth.rotLerp(pt, yRotO, yRot);
        PhyUtils.createRotatorDeg(180.0F - yRotLerp, Vec3f.Y_AXIS, general_rotation);
        armature.setPose(animator.getPose(pt));
        dynamicChains.forEach(dbc -> dbc.feedBack(dx, dy, dz));
        staticBones.forEach((c, j) -> {
            var jid = armature.searchJointByName(j).getId();
            var T = armature.getPoseMatrices()[jid];
            var jtTf = OpenMatrix4f.mul(general_rotation, T, COLLIDER_TRANSFORM);
            //var JT_joml = OpenMatrix4f.exportToMojangMatrix(jtTf);
            var pos = OpenMatrix4f.transform3v(jtTf, Vec3f.ZERO, new Vec3f())/*.add(dx, dy, dz)*/;
            var rot = jtTf.toQuaternion().invert().normalize();
            //.rotateAxis((180.0F - yRotLerp) / 180f * Mth.PI, 0,1,0)
            c.setPose(pos.x, pos.y, pos.z,
                    rot.x, rot.y, rot.z, rot.w);
        });

        dynamicChains.forEach((c) -> {
            var pjid = armature.searchJointByName(c.parentJoint).getId();
            var pT = armature.getPoseMatrices()[pjid];
            var T = new OpenMatrix4f(pT).mulBack(c.headLocal);
            var jtTf = OpenMatrix4f.mul(general_rotation, T, COLLIDER_TRANSFORM);
            //var JT_joml = OpenMatrix4f.exportToMojangMatrix(jtTf);
            var pos = OpenMatrix4f.transform3v(jtTf, Vec3f.ZERO, new Vec3f())/*.add(dx, dy, dz)*/;
            var rot = jtTf.toQuaternion().invert().normalize();
            //.rotateAxis((180.0F - yRotLerp) / 180f * Mth.PI, 0,1,0)
            //c.posCache.set(pos.x, pos.y, pos.z);
            c.headBody.setPose(pos.x, pos.y, pos.z,
                    rot.x, rot.y, rot.z, rot.w);
        });

        if(warmuped){
            jointSprings.forEach(JointSpring::feedback);
            return;
        }

        dynamicChains.forEach(dynamicBoneChain -> {
            for (int i = 0; i < dynamicBoneChain.jointChain.length; i++) {
                var c = dynamicBoneChain.bodyChain[i];
                var jid = armature.searchJointByName(dynamicBoneChain.jointChain[i]).getId();
                var T = armature.getPoseMatrices()[jid];
                var jtTf = OpenMatrix4f.mul(general_rotation, T, COLLIDER_TRANSFORM);
                //var JT_joml = OpenMatrix4f.exportToMojangMatrix(jtTf);
                var pos = OpenMatrix4f.transform3v(jtTf, Vec3f.ZERO, new Vec3f())/*.add(dx, dy, dz)*/;
                var rot = jtTf.toQuaternion().invert().normalize();
                //.rotateAxis((180.0F - yRotLerp) / 180f * Mth.PI, 0,1,0)
                c.setPose(pos.x, pos.y, pos.z,
                        rot.x, rot.y, rot.z, rot.w);
            }
        });

        dynamicChains.forEach(dbc -> {
            for (DynamicBoneCollider dynamicBoneCollider : dbc.bodyChain) {
                dynamicBoneCollider.updatePoseCache();
            }
        });

        warmuped = true;
    }

    protected boolean warmuped = false;
    public void updateStaticBones(float dx, float dy, float dz, float yRot,
                                  Armature armature, Pose pose){
        //float yRotLerp = entityPatch.getYRot();
        PhyUtils.createRotatorDeg(180.0F - yRot, Vec3f.Y_AXIS, general_rotation);
        armature.setPose(pose);

        dynamicChains.forEach(dbc -> dbc.feedBack(dx, dy, dz));

        staticBones.forEach((c, j) -> {
            var jid = armature.searchJointByName(j).getId();
            var T = armature.getPoseMatrices()[jid];
            var jtTf = OpenMatrix4f.mul(general_rotation, T, COLLIDER_TRANSFORM);
            //var JT_joml = OpenMatrix4f.exportToMojangMatrix(jtTf);
            var pos = OpenMatrix4f.transform3v(jtTf, Vec3f.ZERO, new Vec3f())/*.add(dx, dy, dz)*/;
            var rot = jtTf.toQuaternion().invert().normalize();
            //.rotateAxis((180.0F - yRotLerp) / 180f * Mth.PI, 0,1,0)
            c.setPose(pos.x, pos.y, pos.z,
                    rot.x, rot.y, rot.z, rot.w);
        });

        // chain head
        dynamicChains.forEach((c) -> {
            var pjid = armature.searchJointByName(c.parentJoint).getId();
            var pT = armature.getPoseMatrices()[pjid];
            var T = new OpenMatrix4f(pT).mulBack(c.headLocal);
            var jtTf = OpenMatrix4f.mul(general_rotation, T, COLLIDER_TRANSFORM);
            //var JT_joml = OpenMatrix4f.exportToMojangMatrix(jtTf);
            var pos = OpenMatrix4f.transform3v(jtTf, Vec3f.ZERO, new Vec3f())/*.add(dx, dy, dz)*/;
            var rot = jtTf.toQuaternion().invert().normalize();
            //.rotateAxis((180.0F - yRotLerp) / 180f * Mth.PI, 0,1,0)
            //c.posCache.set(pos.x, pos.y, pos.z);
            c.headBody.setPose(pos.x, pos.y, pos.z,
                    rot.x, rot.y, rot.z, rot.w);
        });

        if(warmuped){
            jointSprings.forEach(JointSpring::feedback);
            return;
        }

        dynamicChains.forEach(dynamicBoneChain -> {
            for (int i = 0; i < dynamicBoneChain.jointChain.length; i++) {
                var c = dynamicBoneChain.bodyChain[i];
                var jid = armature.searchJointByName(dynamicBoneChain.jointChain[i]).getId();
                var T = armature.getPoseMatrices()[jid];
                var jtTf = OpenMatrix4f.mul(general_rotation, T, COLLIDER_TRANSFORM);
                //var JT_joml = OpenMatrix4f.exportToMojangMatrix(jtTf);
                var pos = OpenMatrix4f.transform3v(jtTf, Vec3f.ZERO, new Vec3f())/*.add(dx, dy, dz)*/;
                var rot = jtTf.toQuaternion().invert().normalize();
                //.rotateAxis((180.0F - yRotLerp) / 180f * Mth.PI, 0,1,0)
                c.setPose(pos.x, pos.y, pos.z,
                        rot.x, rot.y, rot.z, rot.w);
            }
        });
        dynamicChains.forEach(dbc -> {
            for (DynamicBoneCollider dynamicBoneCollider : dbc.bodyChain) {
                dynamicBoneCollider.updatePoseCache();
            }
        });

        warmuped = true;
    }

    public void tick(float dx,float dy,float dz, float yRot, Armature armature, Pose pose){
        updateStaticBones(-dx, -dy, -dz, yRot, armature, pose);
        physicsSpace.update(0.05f, 10);
        //var rootTransform = OpenMatrix4f.createRotatorDeg(180.0F - yRot, Vec3f.Y_AXIS);
        dynamicChains.forEach(dbc -> {
            for (DynamicBoneCollider dynamicBoneCollider : dbc.bodyChain) {
                dynamicBoneCollider.updatePoseCache();
            }
        });
    }

    public void tick(float _dx,float _dy,float _dz, float yRotO, float yRot, Armature armature, Animator animator){
        float h = 0.01f;
        float dx = _dx / 5;
        float dy = _dy / 5;
        float dz = _dz / 5;
        for (int i = 0; i < 5; i++) {
            updateStaticBonesStep(-dx, -dy, -dz, yRotO, yRot,armature, animator, (i+1) * 0.2f);
            physicsSpace.update(h, 0);
        }
        dynamicChains.forEach(dbc -> {
            for (DynamicBoneCollider dynamicBoneCollider : dbc.bodyChain) {
                dynamicBoneCollider.updatePoseCache();
            }
        });
    }


    private final Matrix4f M = new Matrix4f();
    private final Vec3f T = new Vec3f();
    private final Vec3f S = new Vec3f();
    private final Quaternionf R = new Quaternionf();


    public OpenMatrix4f getAnimationBoundMatrix(Joint joint, OpenMatrix4f parentTransform) {
        AnimationTransformEntry animationTransformEntry = new AnimationTransformEntry();

        animationTransformEntry.put(JOINT_LOCAL_TRANSFORM, joint.getLocalTransform());
        animationTransformEntry.put(PARENT, parentTransform);

        return animationTransformEntry.getResult();
    }

    private final OpenMatrix4f general_rotation = new OpenMatrix4f();
    private void getPoseTransform(Armature armature, Joint _joint, OpenMatrix4f parentTransform,
                                  Pose pose, OpenMatrix4f[] jointMatrices, float pt) {
        var joint = armature.searchJointByName(_joint.getName());
        var jtName = joint.getName();
        OpenMatrix4f result;
        if (dynamicBones.containsKey(jtName)){
            var body = dynamicBones.get(jtName);
            OpenMatrix4f Tf = getAnimationBoundMatrix(joint, parentTransform);
            Tf.toTranslationVector(T);
            Tf.toScaleVector(S);
            body.getRotation(pt, R).premul(yRotCache);
            var jtTf = new JointTransform(T, R.invert(), S);
            result = jtTf.toMatrix();
            jointMatrices[joint.getId()] = result;
        }
        else {
            result = pose.orElseEmpty(joint.getName()).getAnimationBoundMatrix(joint, parentTransform);
            jointMatrices[joint.getId()] = result;
        }

        for (Joint joints : joint.getSubJoints()) {
            this.getPoseTransform(armature, joints, result, pose, jointMatrices, pt);
        }
    }



    private final Quaternionf yRotCache = new Quaternionf();
    public void feedbackPose(float yRot, Armature armature, Pose pose, float pt){
        //float yRotLerp = yRot;
        yRotCache.fromAxisAngleDeg(0, 1, 0, -180.0F + yRot);
        getPoseTransform(armature, armature.rootJoint, OpenMatrix4f.IDENTITY, pose, armature.getPoseMatrices(), pt);
    }

    public void destroy(){
        physicsSpace.destroy();
    }

}
