package dfdyz.ef_anim_phy.physics.bodies;

import org.joml.Math;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;

public class PhyUtils {

    public static boolean ValidFinite(float f){
        return !(Float.isInfinite(f) || Float.isNaN(f));
    }

    public static boolean ValidFinite(float f1, float f2, float f3){
        return ValidFinite(f1) && ValidFinite(f2) && ValidFinite(f3);
    }

    public static boolean ValidFinite(float f1, float f2, float f3, float f4){
        return ValidFinite(f1) && ValidFinite(f2) && ValidFinite(f3) && ValidFinite(f4);
    }

    public static OpenMatrix4f createRotatorDeg(float degree, Vec3f axis, OpenMatrix4f dest) {
        return OpenMatrix4f.rotate(Math.toRadians(degree), axis, new OpenMatrix4f(), dest);
    }

}
