package io.wispforest.owo.ui.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Helper interface implemented on top of the {@link GuiGraphics} to allow for easier matrix stack transformations
 */
public interface MatrixStackTransformer {

    default MatrixStackTransformer translate(double x, double y, double z) {
        this.getPose().translate(x, y, z);
        return this;
    }

    default MatrixStackTransformer translate(float x, float y, float z) {
        this.getPose().translate(x, y, z);
        return this;
    }

    default MatrixStackTransformer scale(float x, float y, float z) {
        this.getPose().scale(x, y, z);
        return this;
    }

    default MatrixStackTransformer multiply(Quaternionf quaternion) {
        this.getPose().mulPose(quaternion);
        return this;
    }

    default MatrixStackTransformer multiply(Quaternionf quaternion, float originX, float originY, float originZ) {
        this.getPose().rotateAround(quaternion, originX, originY, originZ);
        return this;
    }

    default MatrixStackTransformer push() {
        this.getPose().pushPose();
        return this;
    }

    default MatrixStackTransformer pop() {
        this.getPose().popPose();
        return this;
    }

    default MatrixStackTransformer multiplyPositionMatrix(Matrix4f matrix) {
        this.getPose().mulPoseMatrix(matrix);
        return this;
    }

    default PoseStack getPose(){
        throw new IllegalStateException("getMatrices() method hasn't been override leading to exception!");
    }
}
