package dfdyz.ef_anim_phy.physics.bodies;

import com.jme3.bullet.collision.shapes.*;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import yesman.epicfight.api.utils.math.MathUtils;

public class ColliderBody {
    public final PhysicsRigidBody body;

    public final Vector3f posOld = new Vector3f();
    public final Quaternionf rotOld = new Quaternionf();
    public final Vector3f pos = new Vector3f();
    public final Quaternionf rot = new Quaternionf();

    public ColliderBody(PhysicsRigidBody body) {
        this.body = body;
    }

    public ColliderBody(){
        this(new PhysicsRigidBody(new EmptyShape(true)));
        this.body.setKinematic(true);
        this.body.setCollideWithGroups(0xFFFF);
    }

    public void setPose(Vector3f trans, Quaternionf rot){
        setPose(trans.x, trans.y, trans.z, rot.x ,rot.y, rot.z, rot.w);
    }

    public void setPose(float x, float y, float z, float rx, float ry, float rz, float rw){
        if(!(PhyUtils.ValidFinite(x,y,z) && PhyUtils.ValidFinite(rx,ry,rz,rw))) return;
        this.body.setPhysicsLocation(P.set(x, y, z));
        this.body.setPhysicsRotation(R.set(rx ,ry, rz, rw));
    }

    private final com.jme3.math.Vector3f P = new com.jme3.math.Vector3f();
    private final Quaternion R = new Quaternion();
    public void updatePoseCache(){
        posOld.set(pos);
        rotOld.set(rot);
        body.getPhysicsLocation(P);
        body.getPhysicsRotation(R);
        pos.set(P.x, P.y, P.z);
        rot.set(R.getX(), R.getY(), R.getZ(), R.getW());
    }

    public void moveKinematic(float dx, float dy, float dz){
        if(!(PhyUtils.ValidFinite(dx,dy,dz))) return;
        var orig = this.body.getPhysicsLocation(null);
        this.body.setPhysicsLocation(orig.add(dx, dy, dz));
    }

    private void renderDebug(PoseStack poseStack, CollisionShape shape, Matrix4f transform, MultiBufferSource bufferSource){
        var buf = bufferSource.getBuffer(DrawUtils.LINE.apply(1.0));
        if(shape instanceof CapsuleCollisionShape capsuleShape){
            var r = capsuleShape.getRadius();
            var h = capsuleShape.getHeight();
            var axis = capsuleShape.getAxis();
            DrawUtils.renderCylinder(poseStack, buf, transform, Minecraft.getInstance().gameRenderer.getMainCamera(), r, h, axis, 16, 0xFFFF00FF);
        } else if (shape instanceof BoxCollisionShape boxShape) {
            var half = boxShape.getHalfExtents(new com.jme3.math.Vector3f());
            DrawUtils.renderBox(poseStack, buf, transform, Minecraft.getInstance().gameRenderer.getMainCamera(),
                    half.x, half.y, half.z, new Vector3f(), 0xFFFF00FF);
        } else if (shape instanceof SphereCollisionShape ball) {
            DrawUtils.renderBall(poseStack, buf, transform, Minecraft.getInstance().gameRenderer.getMainCamera(),
                    ball.getRadius(), new Vector3f(), 16, 0xFFFF00FF);
        } else {
            DrawUtils.renderBall(poseStack, buf, transform,
                    Minecraft.getInstance().gameRenderer.getMainCamera(), 0.02f, new Vector3f(), 16, 0xFFFFFFFF);
        }
    }

    public void renderDebug(PoseStack poseStack, Vec3 entityPos, MultiBufferSource bufferSource, float pt){
        var mat = new Matrix4f();
        //var t = body.getPhysicsLocation(new com.jme3.math.Vector3f());
        var lerped = MathUtils.lerpMojangVector(posOld, pos, pt);
        var q = getRotation(pt, new Quaternionf());
        mat.rotateLocal(q);
        mat.translateLocal(lerped.x, lerped.y, lerped.z).scaleLocal(2f);
        mat.translateLocal((float) entityPos.x, (float) entityPos.y, (float) entityPos.z);
        /*mat.setTranslation(
                (float) entityPos.x + t.x,
                (float) entityPos.y + t.y,
                (float) entityPos.z + t.z);*/
        renderDebug(poseStack, body.getCollisionShape(),
                mat, bufferSource);
    }

    public void setGroup(int id){
        body.setCollisionGroup(0x0001 << (id % 16));
    }

    public void setMask(int mask){
        body.setCollideWithGroups(mask);
    }

    public int getMask(){
        return body.getCollideWithGroups();
    }

    public void setMaskTo(int id, boolean collision){
        int g = 0x0001 << (id % 16);
        if(collision){
            setMask(getMask() | g);
        }
        else {
            setMask(getMask() & (~g));
        }
    }

    public Quaternionf getRotation(float pt, Quaternionf dist){
        return MathUtils.lerpQuaternion(rotOld, rot, pt, dist);
    }

}
