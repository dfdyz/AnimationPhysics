package dfdyz.ef_anim_phy.physics.bodies;

import com.jme3.bullet.joints.Constraint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;

@FunctionalInterface
public interface JointConstructor<C extends Constraint> {

    C[] create(String boneName, PhysicsRigidBody A, PhysicsRigidBody B, Vector3f povA, Vector3f povB);

}
