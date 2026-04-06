package dfdyz.ef_anim_phy.physics.bodies;

import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import org.joml.Quaternionf;

public class StaticBoneCollider extends ColliderBody {

    public final org.joml.Vector3f offsetPos = new org.joml.Vector3f();
    public static final org.joml.Vector3f offsetCache = new org.joml.Vector3f();
    protected final Quaternionf offsetRot = new Quaternionf();
    protected final Quaternionf rotCache = new Quaternionf();


    @Override
    public void setPose(float x, float y, float z, float rx, float ry, float rz, float rw) {
        if(!(PhyUtils.ValidFinite(x,y,z) && PhyUtils.ValidFinite(rx,ry,rz,rw))) return;

        rotCache.set(rx ,ry, rz, rw);
        offsetCache.set(offsetPos).rotate(rotCache).add(x,y,z);

        this.body.setPhysicsLocation(new com.jme3.math.Vector3f(offsetCache.x, offsetCache.y, offsetCache.z));
        this.body.setPhysicsRotation(new Quaternion(offsetRot.x, offsetRot.y, offsetRot.z, offsetRot.w).multLocal(
                rx ,ry, rz, rw
        ));
    }

    public StaticBoneCollider() {
        super(new PhysicsRigidBody(new CapsuleCollisionShape(0.2f, 0.3f, 0), 1000));
    }

    public StaticBoneCollider(CollisionShape shape){
        super(new PhysicsRigidBody(shape));
    }

    public void init(){
        this.body.setKinematic(true);
    }
}
