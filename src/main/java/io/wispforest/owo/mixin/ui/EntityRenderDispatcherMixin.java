package io.wispforest.owo.mixin.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.wispforest.owo.util.pond.OwoEntityRenderDispatcherExtension;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin implements OwoEntityRenderDispatcherExtension {

    @Unique
    private boolean owo$showNametag = true;
    @Unique
    private boolean owo$counterRotate = false;

    @Override
    public void owo$setShowNametag(boolean showNametag) {
        this.owo$showNametag = showNametag;
    }

    @Override
    public boolean owo$showNametag() {
        return this.owo$showNametag;
    }

    @Override
    public void owo$setCounterRotate(boolean counterRotate) {
        this.owo$counterRotate = counterRotate;
    }

    @Override
    public boolean owo$counterRotate() {
        return this.owo$counterRotate;
    }

    @Shadow public Camera camera;

    @Inject(method = "renderFlame", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V", shift = At.Shift.AFTER))
    private void cancelFireRotation(PoseStack matrices, MultiBufferSource vertexConsumers, Entity entity, Quaternionf rotation, CallbackInfo ci) {
        if (!this.owo$counterRotate) return;
        matrices.mulPose(Axis.YP.rotationDegrees(this.camera.getYRot() + 170));
        matrices.translate(0, 0, .1);
    }
}
