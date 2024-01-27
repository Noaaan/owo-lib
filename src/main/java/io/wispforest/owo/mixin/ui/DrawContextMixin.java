package io.wispforest.owo.mixin.ui;

import com.mojang.blaze3d.vertex.BufferBuilder;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.util.pond.OwoBufferBuilderExtension;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(GuiGraphics.class)
public class DrawContextMixin {

    @SuppressWarnings("ConstantValue")
    @Inject(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;begin(Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;Lcom/mojang/blaze3d/vertex/VertexFormat;)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void injectBufferBegin(ResourceLocation texture, int x1, int x2, int y1, int y2, int z, float u1, float u2, float v1, float v2, CallbackInfo ci, Matrix4f matrix4f, BufferBuilder bufferBuilder) {
        if (!((Object) this instanceof OwoUIDrawContext context) || !context.recording()) return;

        if (bufferBuilder.building()) {
            ((OwoBufferBuilderExtension) bufferBuilder).owo$skipNextBegin();
        }
    }

    @SuppressWarnings("ConstantValue")
    @Inject(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFFFFFF)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;begin(Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;Lcom/mojang/blaze3d/vertex/VertexFormat;)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void injectBufferBeginPartTwo(ResourceLocation texture, int x1, int x2, int y1, int y2, int z, float u1, float u2, float v1, float v2, float red, float green, float blue, float alpha, CallbackInfo ci, Matrix4f matrix4f, BufferBuilder bufferBuilder) {
        if (!((Object) this instanceof OwoUIDrawContext context) || !context.recording()) return;

        if (bufferBuilder.building()) {
            ((OwoBufferBuilderExtension) bufferBuilder).owo$skipNextBegin();
        }
    }

    @SuppressWarnings("ConstantValue")
    @Inject(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;end()Lcom/mojang/blaze3d/vertex/BufferBuilder$RenderedBuffer;"), cancellable = true)
    private void skipDraw(ResourceLocation texture, int x1, int x2, int y1, int y2, int z, float u1, float u2, float v1, float v2, CallbackInfo ci) {
        if ((Object) this instanceof OwoUIDrawContext context && context.recording()) ci.cancel();
    }

    @SuppressWarnings("ConstantValue")
    @Inject(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFFFFFF)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;end()Lcom/mojang/blaze3d/vertex/BufferBuilder$RenderedBuffer;"), cancellable = true)
    private void skipDrawSeason2(ResourceLocation texture, int x1, int x2, int y1, int y2, int z, float u1, float u2, float v1, float v2, float red, float green, float blue, float alpha, CallbackInfo ci) {
        if ((Object) this instanceof OwoUIDrawContext context && context.recording()) ci.cancel();
    }
}
