package io.wispforest.owo.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.owo.ui.util.MatrixStackTransformer;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.*;

@Mixin(GuiGraphics.class)
public abstract class DrawContextMixin implements MatrixStackTransformer {

    @Shadow public abstract PoseStack pose();

    @Override
    public PoseStack getPose() {
        return pose();
    }
}
