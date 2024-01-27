package io.wispforest.owo.mixin.ui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;

@Mixin(GuiGraphics.class)
public interface DrawContextInvoker {

    @Invoker("renderTooltipInternal")
    void owo$renderTooltipFromComponents(Font textRenderer, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner);

    @Accessor("pose")
    PoseStack owo$getMatrices();

    @Mutable
    @Accessor("pose")
    void owo$setMatrices(PoseStack matrices);

    @Accessor("scissorStack")
    GuiGraphics.ScissorStack owo$getScissorStack();

    @Mutable
    @Accessor("scissorStack")
    void owo$setScissorStack(GuiGraphics.ScissorStack scissorStack);
}
