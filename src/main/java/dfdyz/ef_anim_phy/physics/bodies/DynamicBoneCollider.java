package dfdyz.ef_anim_phy.physics.bodies;

import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;

public class DynamicBoneCollider extends ColliderBody {
    public DynamicBoneCollider(CollisionShape shape, float mass) {
        super(new PhysicsRigidBody(shape, mass));
        body.setDamping(0.2f, 0.2f);
    }
}
