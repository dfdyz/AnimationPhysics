package dfdyz.ef_anim_phy.physics.bodies;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.joints.Constraint;
import com.jme3.bullet.joints.New6Dof;
import com.jme3.bullet.joints.motors.MotorParam;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.jme3.bullet.RotationOrder.ZXY;

public class DynamicBoneChain<C extends Constraint> {
    public final String parentJoint;
    public float firstLengthHalf;
    //public final Vector3f posCache = new Vector3f();
    public final ColliderBody headBody;
    public final OpenMatrix4f headLocal;
    public final DynamicBoneCollider[] bodyChain;
    public final String[] jointChain;

    public DynamicBoneChain(PhysicsSpace world, Armature armature, String[] chain, float[] mass,
                            float[] move_damp, float[] rot_damp, CollisionShape[] shape,
                            int[] group, int[] mask, float rare_len,
                            JointConstructor<C> constructor){
        this.jointChain = chain;

        var poses = armature.getPoseMatrices();
        this.headBody = new ColliderBody();

        var head_joint = armature.searchJointByName(chain[0]);
        this.headLocal = new OpenMatrix4f(head_joint.getLocalTransform());
        var head_initial = poses[head_joint.getId()];

        parentJoint = findParent(armature, head_joint).getName();
        bodyChain = new DynamicBoneCollider[chain.length];

        var headInitPos = head_initial.toTranslationVector();
        var headInitRot = head_initial.toQuaternion().invert().normalize();

        headBody.setPose(
                headInitPos.x, headInitPos.y, headInitPos.z,
                headInitRot.x, headInitRot.y, headInitRot.z, headInitRot.w
        );

        float lastLen = 0;
        for (int i = 0; i < bodyChain.length-1; i++) {
            var sj = armature.searchJointByName(chain[i]);
            var ej = armature.searchJointByName(chain[i+1]);
            var startT = poses[sj.getId()];
            var arm = ej.getLocalTransform().toTranslationVector();

            var halfArm = arm.copy().scale(0.5f);
            var midP = OpenMatrix4f.transform3v(startT, halfArm, new Vec3f());
            var segmentLen = arm.length();

            var initRot = startT.toQuaternion().invert().normalize();//OpenMatrix4f.exportToMojangMatrix(startT).getNormalizedRotation(new Quaternionf());

            var dbc = new DynamicBoneCollider(shape[i], mass[i]);

            bodyChain[i] = dbc;
            dbc.setPose(
                    midP.x, midP.y, midP.z,
                    initRot.x, initRot.y, initRot.z, initRot.w
            );

            // create constraint
            PhysicsRigidBody A, B;
            B = dbc.body;
            B.setDamping(move_damp[i], rot_damp[i]);
            dbc.setGroup(group[i]);
            dbc.setMask(mask[i]);
            world.addCollisionObject(B);
            if(i == 0) {
                A = headBody.body;
                world.addCollisionObject(A);
            }
            else {
                A = bodyChain[i-1].body;
            }

            //A.addToIgnoreList(B);

            com.jme3.math.Vector3f povA, povB;
            povA = new com.jme3.math.Vector3f(0, lastLen / 2, 0);
            povB = new com.jme3.math.Vector3f(0, -segmentLen / 2, 0);

            //New6Dof(A, B, povA, povB, Matrix3f.IDENTITY, Matrix3f.IDENTITY, ZXY);
            var links = constructor.create(chain[i], A, B, povA, povB);
            for (C link : links) {
                world.addJoint(link);
            }
            if(i == 0){
                firstLengthHalf = segmentLen/2;
            }
            lastLen = segmentLen;
        }

        // final segment
        int i = chain.length-1;
        var sj = armature.searchJointByName(chain[i]);
        var startT = poses[sj.getId()];
        var startP = startT.toTranslationVector();
        var segmentLen = rare_len;
        var midP = OpenMatrix4f.transform3v(startT, Vec3f.Y_AXIS, new Vec3f()).scale(
                segmentLen / 2).add(startP);

        var initRot = startT.toQuaternion().invert().normalize();//OpenMatrix4f.exportToMojangMatrix(startT).getNormalizedRotation(new Quaternionf());
        var dbc = new DynamicBoneCollider(shape[i], mass[i]);
        bodyChain[i] = dbc;
        dbc.setPose(
                midP.x, midP.y, midP.z,
                initRot.x, initRot.y, initRot.z, initRot.w
        );

        // create constraint
        PhysicsRigidBody A, B;
        B = dbc.body;
        B.setDamping(move_damp[i], rot_damp[i]);
        dbc.setGroup(group[i]);
        dbc.setMask(mask[i]);
        world.addCollisionObject(B);
        if(i == 0) {
            A = headBody.body;
            world.addCollisionObject(A);
        }
        else {
            A = bodyChain[i-1].body;
        }

        com.jme3.math.Vector3f povA, povB;
        povA = new com.jme3.math.Vector3f(0, lastLen / 2, 0);
        povB = new com.jme3.math.Vector3f(0, -segmentLen / 2, 0);
        var links = constructor.create(chain[i], A, B, povA, povB);
        for (C link : links) {
            world.addJoint(link);
        }
    }

    public void updateArmature(Armature armature){
        for (int i = 0; i < bodyChain.length; i++) {
            var dbc = bodyChain[i];
            var joint = armature.searchJointByName(jointChain[i]);

        }
    }

    protected Joint findParent(Armature armature, Joint target){
        return findParent(armature.rootJoint, target);
    }

    protected Joint findParent(Joint current, Joint target){
        for (Joint subJoint : current.getSubJoints()) {
            if(subJoint.getId() == target.getId()) return current;
            else {
                var result = findParent(subJoint, target);
                if(result != null) return result;
            }
        }
        return null;
    }

    public void renderDebug(PoseStack poseStack, Vec3 entityPos, MultiBufferSource bufferSource, float pt){
        headBody.renderDebug(poseStack, entityPos, bufferSource, pt);
        for (DynamicBoneCollider dynamicBoneCollider : bodyChain) {
            dynamicBoneCollider.renderDebug(poseStack, entityPos, bufferSource, pt);
        }
    }

    public void setGroup(int id){
        for (DynamicBoneCollider dynamicBoneCollider : bodyChain) {
            dynamicBoneCollider.setGroup(id);
        }
    }

    public void setMask(int mask){
        for (DynamicBoneCollider dynamicBoneCollider : bodyChain) {
            dynamicBoneCollider.setMask(mask);
        }
    }

    private final com.jme3.math.Vector3f imp = new com.jme3.math.Vector3f();
    public void feedBack(float dx, float dy, float dz){
        for (DynamicBoneCollider dynamicBoneCollider : bodyChain) {
            //dynamicBoneCollider.moveKinematic(dx, dy, dz);
            float mass=dynamicBoneCollider.body.getMass();
            dynamicBoneCollider.body.applyCentralImpulse(imp.set(dx, dy, dz).mult(mass));
        }
    }

    private final Quaternionf qCache = new Quaternionf();
    private final OpenMatrix4f mCache = new OpenMatrix4f();

    // for test
    /*public DynamicBoneChain(PhysicsSpace world, Armature armature, int group, int mask, float radius, String... chain){
        this.jointChain = chain;

        var poses = armature.getPoseMatrices();
        this.headBody = new ColliderBody();

        var head_joint = armature.searchJointByName(chain[0]);
        this.headLocal = new OpenMatrix4f(head_joint.getLocalTransform());
        var head_initial = poses[head_joint.getId()];

        parentJoint = findParent(armature, head_joint).getName();
        bodyChain = new DynamicBoneCollider[chain.length];

        var headInitPos = head_initial.toTranslationVector();
        var headInitRot = head_initial.toQuaternion().invert().normalize();//OpenMatrix4f.exportToMojangMatrix(head_initial).getNormalizedRotation(new Quaternionf());

        headBody.setPose(
                headInitPos.x, headInitPos.y, headInitPos.z,
                headInitRot.x, headInitRot.y, headInitRot.z, headInitRot.w
        );

        float lastLen = 0;
        for (int i = 0; i < bodyChain.length-1; i++) {
            var sj = armature.searchJointByName(chain[i]);
            var ej = armature.searchJointByName(chain[i+1]);
            var startT = poses[sj.getId()];
            var arm = ej.getLocalTransform().toTranslationVector();

            var halfArm = arm.copy().scale(0.5f);
            var midP = OpenMatrix4f.transform3v(startT, halfArm, new Vec3f());
            var segmentLen = arm.length();

            var initRot = startT.toQuaternion().invert().normalize();//OpenMatrix4f.exportToMojangMatrix(startT).getNormalizedRotation(new Quaternionf());

            var dbc = new DynamicBoneCollider(new CapsuleCollisionShape(radius, segmentLen, 1), 0.5f);

            bodyChain[i] = dbc;
            dbc.setPose(
                    midP.x, midP.y, midP.z,
                    initRot.x, initRot.y, initRot.z, initRot.w
            );

            // create constraint
            PhysicsRigidBody A, B;
            B = dbc.body;
            dbc.setGroup(group);
            dbc.setMaskTo(group, false);
            world.addCollisionObject(B);
            if(i == 0) {
                A = headBody.body;
                world.addCollisionObject(A);
            }
            else {
                A = bodyChain[i-1].body;
            }

            //A.addToIgnoreList(B);

            com.jme3.math.Vector3f povA, povB;

            if(i == 0){
                povA = new com.jme3.math.Vector3f();
            }
            else {
                povA = new com.jme3.math.Vector3f(0, lastLen / 2, 0);
            }

            povB = new com.jme3.math.Vector3f(0, -segmentLen / 2, 0);

            var link = new New6Dof(A, B, povA, povB, Matrix3f.IDENTITY, Matrix3f.IDENTITY, ZXY);
            // todo: rotation limit

            link.set(MotorParam.LowerLimit, 3, (float) Math.toRadians(-30)); // X axis
            link.set(MotorParam.UpperLimit, 3, (float) Math.toRadians(30));
            link.set(MotorParam.LowerLimit, 4, (float) Math.toRadians(-2)); // Y axis
            link.set(MotorParam.UpperLimit, 4, (float) Math.toRadians(2));
            link.set(MotorParam.LowerLimit, 5, (float) Math.toRadians(-30)); // Z axis
            link.set(MotorParam.UpperLimit, 5, (float) Math.toRadians(30));

            if(i == 0){
                firstLengthHalf = segmentLen/2;
            }


            world.addJoint(link);
            System.out.format("%s -> %s, l=%.3f", chain[i], chain[i+1], segmentLen);
            lastLen = segmentLen;
        }

        // final segment
        int i = chain.length-1;
        var sj = armature.searchJointByName(chain[i]);
        var startT = poses[sj.getId()];
        var startP = startT.toTranslationVector();
        var segmentLen = 0.5f;
        var midP = OpenMatrix4f.transform3v(startT, Vec3f.Y_AXIS, new Vec3f()).scale(segmentLen / 2).add(startP);

        var initRot = startT.toQuaternion().invert().normalize();//OpenMatrix4f.exportToMojangMatrix(startT).getNormalizedRotation(new Quaternionf());
        var dbc = new DynamicBoneCollider(new CapsuleCollisionShape(radius, segmentLen, 1), 0.5f);
        bodyChain[i] = dbc;
        dbc.setPose(
                midP.x, midP.y, midP.z,
                initRot.x, initRot.y, initRot.z, initRot.w
        );

        // create constraint
        PhysicsRigidBody A, B;
        A = bodyChain[i-1].body;
        B = dbc.body;
        dbc.setGroup(group);
        dbc.setMaskTo(group, false);
        //A.addToIgnoreList(B);
        world.addCollisionObject(B);

        com.jme3.math.Vector3f povA, povB;
        povA = new com.jme3.math.Vector3f(0, lastLen / 2, 0);
        povB = new com.jme3.math.Vector3f(0, -segmentLen / 2, 0);

        var link = new New6Dof(A, B, povA, povB, Matrix3f.IDENTITY, Matrix3f.IDENTITY, ZXY);
        // todo: rotation limit
        link.set(MotorParam.LowerLimit, 3, (float) Math.toRadians(-30)); // X axis
        link.set(MotorParam.UpperLimit, 3, (float) Math.toRadians(30));
        link.set(MotorParam.LowerLimit, 4, (float) Math.toRadians(-2)); // Y axis
        link.set(MotorParam.UpperLimit, 4, (float) Math.toRadians(2));
        link.set(MotorParam.LowerLimit, 5, (float) Math.toRadians(-30)); // Z axis
        link.set(MotorParam.UpperLimit, 5, (float) Math.toRadians(30));
        world.addJoint(link);
        System.out.format("%s -> ?, l=%.3f\n", chain[i], segmentLen);
    }
*/

}
