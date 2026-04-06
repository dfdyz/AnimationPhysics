package dfdyz.ef_anim_phy.physics.bodies;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Function;

public class DrawUtils {

    public static Matrix4f toJoml(com.jme3.math.Matrix4f m){
        return new Matrix4f(
                m.m00, m.m10, m.m20, m.m30,
                m.m01, m.m11, m.m21, m.m31,
                m.m02, m.m12, m.m22, m.m32,
                m.m03, m.m13, m.m23, m.m33
        );
    }


    public static List<Integer> getSideAxes(int normalAxis){
        return switch (normalAxis) {
            case 0 -> List.of(1, 2); // X轴的法线，侧面用Y和Z轴圆环
            case 1 -> List.of(0, 2); // Y轴的法线，侧面用X和Z轴圆环
            case 2 -> List.of(0, 1); // Z轴的法线，侧面用X和Y轴圆环
            default -> List.of(0, 2); // 默认
        };
    }



    public static void renderBox(PoseStack poseStack, VertexConsumer vBuf, Matrix4f transform, Camera camera,
                                 float x, float y, float z, Vector3f centerOffset, int color)
    {
        var poseMatrix = poseStack.last().pose();
        var camPos = camera.getPosition();
        Vector3f[] points = new Vector3f[]{
                new Vector3f(x,y,z),
                new Vector3f(-x,y,z),

                new Vector3f(x,-y,z),
                new Vector3f(-x,-y,z),

                new Vector3f(x,-y,-z),
                new Vector3f(-x,-y,-z),

                new Vector3f(x,y,-z),
                new Vector3f(-x,y,-z),
        };

        for (Vector3f p : points) {
            p.add(centerOffset);
            transform.transformPosition(p, p).sub((float) camPos.x, (float) camPos.y, (float) camPos.z);
        }

        int[] line = new int[]{
                0,1, 2,3, 4,5, 6,7,
                0,2, 1,3, 4,6, 5,7,
                2,4, 3,5, 0,6, 1,7
        };

        for (int i = 0; i < line.length / 2; i++) {
            var from = points[line[i*2]];
            var to = points[line[i*2+1]];
            vBuf.addVertex(poseMatrix, from.x, from.y, from.z)
                    .setColor(color);
            vBuf.addVertex(poseMatrix, to.x, to.y, to.z)
                    .setColor(color);
        }
    }

    public static void renderBall(PoseStack poseStack, VertexConsumer vBuf, Matrix4f transform, Camera camera,
                                  float radius, Vector3f centerOffset, int segments, int color){
        renderCircle(poseStack, vBuf, transform, camera, radius, 0, centerOffset, segments, color);
        renderCircle(poseStack, vBuf, transform, camera, radius, 1, centerOffset, segments, color);
        renderCircle(poseStack, vBuf, transform, camera, radius, 2, centerOffset, segments, color);
    }

    public static void renderCylinder(PoseStack poseStack, VertexConsumer vBuf, Matrix4f transform, Camera camera,
                               float radius, float height, int axis, int segments, int color){

        var poseMatrix = poseStack.last().pose();
        var camPos = camera.getPosition();
        // 顶部圆环
        Vector3f topOff = switch (axis){
            case 0 -> new Vector3f(height / 2f, 0f, 0f);
            case 1 -> new Vector3f(0f, height / 2f, 0f);
            case 2 -> new Vector3f(0f, 0f, height / 2f);
            default -> new Vector3f(0f, height / 2f, 0f);
        };

        //renderCircle(poseStack, vBuf, transform, camera, radius, axis, topOff, segments, color);
        renderBall(poseStack, vBuf, transform, camera, radius, topOff, segments, color);

        // 底部圆环
        Vector3f bottomOff = switch (axis) {
            case  0 -> new Vector3f(-height / 2f, 0f, 0f); // X轴
            case 1 -> new Vector3f(0f, -height / 2f, 0f);// Y轴
            case 2 -> new Vector3f(0f, 0f, -height / 2f); // Z轴
            default -> new Vector3f(0f, -height / 2f, 0f);
        };
        //renderCircle(poseStack, vBuf, transform, camera, radius, axis, bottomOff, segments, color);
        renderBall(poseStack, vBuf, transform, camera, radius, bottomOff, segments, color);

        for (int i = 0; i < segments; i += segments/4) {
            var angle = 2f * Mth.PI * i / segments;

            // 顶部点
            Vector3f topPoint = switch (axis) {
                case  0 -> new Vector3f(height / 2f, radius * Mth.cos(angle), radius * Mth.sin(angle)); // X轴
                case 1 -> new Vector3f(radius * Mth.cos(angle), height / 2f, radius * Mth.sin(angle)); // Y轴
                case 2 -> new Vector3f(radius * Mth.cos(angle), radius * Mth.sin(angle), height / 2f); // Z轴
                default -> new Vector3f(radius * Mth.cos(angle), height / 2f, radius * Mth.sin(angle));
            };

            // 底部点
            Vector3f bottomPoint = switch (axis) {
                case 0 -> new Vector3f(-height / 2f, radius * Mth.cos(angle), radius * Mth.sin(angle)); // X轴
                case 1 -> new Vector3f(radius * Mth.cos(angle), -height / 2f, radius * Mth.sin(angle)); // Y轴
                case  2 -> new Vector3f(radius * Mth.cos(angle), radius * Mth.sin(angle), -height / 2f); // Z轴
                default -> new Vector3f(radius * Mth.cos(angle), -height / 2f, radius * Mth.sin(angle));
            };

            var from = transform.transformPosition(topPoint, new Vector3f()).sub((float) camPos.x, (float) camPos.y, (float) camPos.z);
            var to = transform.transformPosition(bottomPoint, new Vector3f()).sub((float) camPos.x, (float) camPos.y, (float) camPos.z);

            vBuf.addVertex(poseMatrix, from.x, from.y, from.z)
                    .setColor(color);
            vBuf.addVertex(poseMatrix, to.x, to.y, to.z)
                    .setColor(color);
        }

    }

    public static void renderPoint(PoseStack poseStack, VertexConsumer vBuf, Matrix4f transform, Camera camera,
                              float half, int color
    ) {
        var camPos = camera.getPosition();
        var poseMatrix = poseStack.last().pose();

        // x
        var from = transform.transformPosition(new Vector3f(-half,0,0)).sub((float) camPos.x, (float) camPos.y, (float) camPos.z);
        var to = transform.transformPosition(new Vector3f(half,0,0)).sub((float) camPos.x, (float) camPos.y, (float) camPos.z);
        vBuf.addVertex(poseMatrix, from.x, from.y, from.z)
                .setColor(color);
        vBuf.addVertex(poseMatrix, to.x, to.y, to.z)
                .setColor(color);

        // y
        from = transform.transformPosition(from.set(0,-half,0)).sub((float) camPos.x, (float) camPos.y, (float) camPos.z);
        to = transform.transformPosition(from.set(0,half,0)).sub((float) camPos.x, (float) camPos.y, (float) camPos.z);
        vBuf.addVertex(poseMatrix, from.x, from.y, from.z)
                .setColor(color);
        vBuf.addVertex(poseMatrix, to.x, to.y, to.z)
                .setColor(color);

        // z
        from = transform.transformPosition(from.set(0,0,-half)).sub((float) camPos.x, (float) camPos.y, (float) camPos.z);
        to = transform.transformPosition(from.set(0,0,half)).sub((float) camPos.x, (float) camPos.y, (float) camPos.z);
        vBuf.addVertex(poseMatrix, from.x, from.y, from.z)
                .setColor(color);
        vBuf.addVertex(poseMatrix, to.x, to.y, to.z)
                .setColor(color);

    }

    public static void renderCircle(PoseStack poseStack, VertexConsumer vBuf, Matrix4f transform, Camera camera,
                             float radius, int normalAxis, Vector3f centerOffset, int segments, int color
    ) {
        var camPos = camera.getPosition();
        var poseMatrix = poseStack.last().pose();

        for (int i = 0; i < segments; i++) {
            var angle1 = 2f * Mth.PI * i / segments;
            var angle2 = 2f * Mth.PI * (i + 1) / segments;

            var point1 = createCirclePoint(radius, angle1, normalAxis, centerOffset);
            var point2 = createCirclePoint(radius, angle2, normalAxis, centerOffset);

            var from = transform.transformPosition(point1, new Vector3f()).sub((float) camPos.x, (float) camPos.y, (float) camPos.z);
            var to = transform.transformPosition(point2, new Vector3f()).sub((float) camPos.x, (float) camPos.y, (float) camPos.z);
            vBuf.addVertex(poseMatrix, from.x, from.y, from.z)
                    .setColor(color);
            vBuf.addVertex(poseMatrix, to.x, to.y, to.z)
                    .setColor(color);
        }
    }

    public static Vector3f createCirclePoint(float radius, float angle, int normalAxis, Vector3f centerOffset) {
        float cos = Mth.cos(angle) * radius;
        float sin = Mth.sin(angle) * radius;

        return switch (normalAxis) {
            case 0 -> new Vector3f(centerOffset.x, centerOffset.y + cos, centerOffset.z + sin);
            case 1 -> new Vector3f(centerOffset.x + cos, centerOffset.y, centerOffset.z + sin);
            default -> new Vector3f(centerOffset.x + cos, centerOffset.y + sin, centerOffset.z);
        };
    }

    public static final Function<Double, RenderType.CompositeRenderType> LINE = Util.memoize((width) -> {
        return RenderType.create("debug_line_strip",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.DEBUG_LINE_STRIP,
                1536,
                RenderType.CompositeState.builder().setShaderState(RenderType.POSITION_COLOR_SHADER)
                        .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(width))).setTransparencyState(RenderType.
                                NO_TRANSPARENCY).setCullState(RenderType.NO_CULL)
                        .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                        .createCompositeState(false));
    });
}
