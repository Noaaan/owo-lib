package io.wispforest.owo.ui.core;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.wispforest.owo.client.OwoClient;
import io.wispforest.owo.mixin.ui.DrawContextInvoker;
import io.wispforest.owo.ui.event.WindowResizeCallback;
import io.wispforest.owo.ui.util.NinePatchTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2d;
import java.util.ArrayList;
import java.util.List;

public class OwoUIDrawContext extends GuiGraphics {

    @Deprecated
    public static final ResourceLocation PANEL_TEXTURE = new ResourceLocation("owo", "textures/gui/panel.png");
    @Deprecated
    public static final ResourceLocation DARK_PANEL_TEXTURE = new ResourceLocation("owo", "textures/gui/dark_panel.png");
    @Deprecated
    public static final ResourceLocation PANEL_INSET_TEXTURE = new ResourceLocation("owo", "textures/gui/panel_inset.png");

    public static final ResourceLocation PANEL_NINE_PATCH_TEXTURE = new ResourceLocation("owo", "panel/default");
    public static final ResourceLocation DARK_PANEL_NINE_PATCH_TEXTURE = new ResourceLocation("owo", "panel/dark");
    public static final ResourceLocation PANEL_INSET_NINE_PATCH_TEXTURE = new ResourceLocation("owo", "panel/inset");

    private boolean recording = false;

    private OwoUIDrawContext(Minecraft client, MultiBufferSource.BufferSource vertexConsumers) {
        super(client, vertexConsumers);
    }

    public static OwoUIDrawContext of(GuiGraphics context) {
        var owoContext = new OwoUIDrawContext(Minecraft.getInstance(), context.bufferSource());
        ((DrawContextInvoker) owoContext).owo$setScissorStack(((DrawContextInvoker) context).owo$getScissorStack());
        ((DrawContextInvoker) owoContext).owo$setMatrices(((DrawContextInvoker) context).owo$getMatrices());

        return owoContext;
    }

    public static UtilityScreen utilityScreen() {
        return UtilityScreen.get();
    }

    public void recordQuads() {
        recording = true;
    }

    public boolean recording() {
        return recording;
    }

    public void submitQuads() {
        recording = false;
        Tesselator.getInstance().end();
    }

    /**
     * Draw the outline of a rectangle
     *
     * @param x      The x-coordinate of top-left corner of the rectangle
     * @param y      The y-coordinate of top-left corner of the rectangle
     * @param width  The width of the rectangle
     * @param height The height of the rectangle
     * @param color  The color of the rectangle
     */
    public void drawRectOutline(int x, int y, int width, int height, int color) {
        this.fill(x, y, x + width, y + 1, color);
        this.fill(x, y + height - 1, x + width, y + height, color);

        this.fill(x, y + 1, x + 1, y + height - 1, color);
        this.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    /**
     * Draw a filled rectangle with a gradient
     *
     * @param x                The x-coordinate of top-left corner of the rectangle
     * @param y                The y-coordinate of top-left corner of the rectangle
     * @param width            The width of the rectangle
     * @param height           The height of the rectangle
     * @param topLeftColor     The color at the rectangle's top left corner
     * @param topRightColor    The color at the rectangle's top right corner
     * @param bottomRightColor The color at the rectangle's bottom right corner
     * @param bottomLeftColor  The color at the rectangle's bottom left corner
     */
    public void drawGradientRect(int x, int y, int width, int height, int topLeftColor, int topRightColor, int bottomRightColor, int bottomLeftColor) {
        var buffer = Tesselator.getInstance().getBuilder();
        var matrix = this.pose().last().pose();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x + width, y, 0).color(topRightColor).endVertex();
        buffer.vertex(matrix, x, y, 0).color(topLeftColor).endVertex();
        buffer.vertex(matrix, x, y + height, 0).color(bottomLeftColor).endVertex();
        buffer.vertex(matrix, x + width, y + height, 0).color(bottomRightColor).endVertex();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator.getInstance().end();

        RenderSystem.disableBlend();
    }

    /**
     * Draw a panel that looks like the background of a vanilla
     * inventory screen
     *
     * @param x      The x-coordinate of top-left corner of the panel
     * @param y      The y-coordinate of top-left corner of the panel
     * @param width  The width of the panel
     * @param height The height of the panel
     * @param dark   Whether to use the dark version of the panel texture
     */
    public void drawPanel(int x, int y, int width, int height, boolean dark) {
        NinePatchTexture.draw(dark ? DARK_PANEL_NINE_PATCH_TEXTURE : PANEL_NINE_PATCH_TEXTURE, this, x, y, width, height);
    }

    public void drawSpectrum(int x, int y, int width, int height, boolean vertical) {
        var buffer = Tesselator.getInstance().getBuilder();
        var matrix = this.pose().last().pose();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x, y, 0).color(1f, 1f, 1f, 1f).endVertex();
        buffer.vertex(matrix, x, y + height, 0).color(vertical ? 0f : 1f, 1f, 1f, 1f).endVertex();
        buffer.vertex(matrix, x + width, y + height, 0).color(0f, 1f, 1f, 1f).endVertex();
        buffer.vertex(matrix, x + width, y, 0).color(vertical ? 1f : 0f, 1f, 1f, 1f).endVertex();

        OwoClient.HSV_PROGRAM.use();
        Tesselator.getInstance().end();
    }

    public void drawText(Component text, float x, float y, float scale, int color) {
        drawText(text, x, y, scale, color, TextAnchor.TOP_LEFT);
    }

    public void drawText(Component text, float x, float y, float scale, int color, TextAnchor anchorPoint) {
        final var textRenderer = Minecraft.getInstance().font;

        this.pose().pushPose();
        this.pose().scale(scale, scale, 1);

        switch (anchorPoint) {
            case TOP_RIGHT -> x -= textRenderer.width(text) * scale;
            case BOTTOM_LEFT -> y -= textRenderer.lineHeight * scale;
            case BOTTOM_RIGHT -> {
                x -= textRenderer.width(text) * scale;
                y -= textRenderer.lineHeight * scale;
            }
        }


        this.drawString(textRenderer, text, (int) (x * (1 / scale)), (int) (y * (1 / scale)), color, false);
        this.pose().popPose();
    }

    public enum TextAnchor {
        TOP_RIGHT, BOTTOM_RIGHT, TOP_LEFT, BOTTOM_LEFT
    }

    public void drawLine(int x1, int y1, int x2, int y2, double thiccness, Color color) {
        var offset = new Vector2d(x2 - x1, y2 - y1).perpendicular().normalize().mul(thiccness * .5d);

        var buffer = Tesselator.getInstance().getBuilder();
        var matrix = this.pose().last().pose();
        int vColor = color.argb();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        buffer.vertex(matrix, (float) (x1 + offset.x), (float) (y1 + offset.y), 0).color(vColor).endVertex();
        buffer.vertex(matrix, (float) (x1 - offset.x), (float) (y1 - offset.y), 0).color(vColor).endVertex();
        buffer.vertex(matrix, (float) (x2 - offset.x), (float) (y2 - offset.y), 0).color(vColor).endVertex();
        buffer.vertex(matrix, (float) (x2 + offset.x), (float) (y2 + offset.y), 0).color(vColor).endVertex();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator.getInstance().end();
    }

    public void drawCircle(int centerX, int centerY, int segments, double radius, Color color) {
        drawCircle(centerX, centerY, 0, 360, segments, radius, color);
    }

    public void drawCircle(int centerX, int centerY, double angleFrom, double angleTo, int segments, double radius, Color color) {
        Preconditions.checkArgument(angleFrom < angleTo, "angleFrom must be less than angleTo");

        var buffer = Tesselator.getInstance().getBuilder();
        var matrix = this.pose().last().pose();

        double angleStep = Math.toRadians(angleTo - angleFrom) / segments;
        int vColor = color.argb();

        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, centerX, centerY, 0).color(vColor).endVertex();

        for (int i = segments; i >= 0; i--) {
            double theta = Math.toRadians(angleFrom) + i * angleStep;
            buffer.vertex(matrix, (float) (centerX - Math.cos(theta) * radius), (float) (centerY - Math.sin(theta) * radius), 0)
                    .color(vColor).endVertex();
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator.getInstance().end();
    }

    public void drawRing(int centerX, int centerY, int segments, double innerRadius, double outerRadius, Color innerColor, Color outerColor) {
        drawRing(centerX, centerY, 0d, 360d, segments, innerRadius, outerRadius, innerColor, outerColor);
    }

    public void drawRing(int centerX, int centerY, double angleFrom, double angleTo, int segments, double innerRadius, double outerRadius, Color innerColor, Color outerColor) {
        Preconditions.checkArgument(angleFrom < angleTo, "angleFrom must be less than angleTo");
        Preconditions.checkArgument(innerRadius < outerRadius, "innerRadius must be less than outerRadius");

        var buffer = Tesselator.getInstance().getBuilder();
        var matrix = this.pose().last().pose();

        double angleStep = Math.toRadians(angleTo - angleFrom) / segments;
        int inColor = innerColor.argb();
        int outColor = outerColor.argb();

        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i <= segments; i++) {
            double theta = Math.toRadians(angleFrom) + i * angleStep;

            buffer.vertex(matrix, (float) (centerX - Math.cos(theta) * outerRadius), (float) (centerY - Math.sin(theta) * outerRadius), 0)
                    .color(outColor).endVertex();
            buffer.vertex(matrix, (float) (centerX - Math.cos(theta) * innerRadius), (float) (centerY - Math.sin(theta) * innerRadius), 0)
                    .color(inColor).endVertex();
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator.getInstance().end();
    }

    public void drawTooltip(Font textRenderer, int x, int y, List<ClientTooltipComponent> components) {
        ((DrawContextInvoker) this).owo$renderTooltipFromComponents(textRenderer, components, x, y, DefaultTooltipPositioner.INSTANCE);
    }

    // --- debug rendering ---

    /**
     * Draw the area around the given rectangle which
     * the given insets describe
     *
     * @param x      The x-coordinate of top-left corner of the rectangle
     * @param y      The y-coordinate of top-left corner of the rectangle
     * @param width  The width of the rectangle
     * @param height The height of the rectangle
     * @param insets The insets to draw around the rectangle
     * @param color  The color to draw the inset area with
     */
    public void drawInsets(int x, int y, int width, int height, Insets insets, int color) {
        this.fill(x - insets.left(), y - insets.top(), x + width + insets.right(), y, color);
        this.fill(x - insets.left(), y + height, x + width + insets.right(), y + height + insets.bottom(), color);

        this.fill(x - insets.left(), y, x, y + height, color);
        this.fill(x + width, y, x + width + insets.right(), y + height, color);
    }

    /**
     * Draw the element inspector for the given tree, detailing the position,
     * bounding box, margins and padding of each component
     *
     * @param root        The root component of the hierarchy to draw
     * @param mouseX      The x-coordinate of the mouse pointer
     * @param mouseY      The y-coordinate of the mouse pointer
     * @param onlyHovered Whether to only draw the inspector for the hovered widget
     */
    public void drawInspector(ParentComponent root, double mouseX, double mouseY, boolean onlyHovered) {
        RenderSystem.disableDepthTest();
        var client = Minecraft.getInstance();
        var textRenderer = client.font;

        var children = new ArrayList<io.wispforest.owo.ui.core.Component>();
        if (!onlyHovered) {
            root.collectDescendants(children);
        } else if (root.childAt((int) mouseX, (int) mouseY) != null) {
            children.add(root.childAt((int) mouseX, (int) mouseY));
        }

        for (var child : children) {
            if (child instanceof ParentComponent parentComponent) {
                this.drawInsets(parentComponent.x(), parentComponent.y(), parentComponent.width(),
                        parentComponent.height(), parentComponent.padding().get().inverted(), 0xA70CECDD);
            }

            final var margins = child.margins().get();
            this.drawInsets(child.x(), child.y(), child.width(), child.height(), margins, 0xA7FFF338);
            drawRectOutline(child.x(), child.y(), child.width(), child.height(), 0xFF3AB0FF);

            if (onlyHovered) {

                int inspectorX = child.x() + 1;
                int inspectorY = child.y() + child.height() + child.margins().get().bottom() + 1;
                int inspectorHeight = textRenderer.lineHeight * 2 + 4;

                if (inspectorY > client.getWindow().getGuiScaledHeight() - inspectorHeight) {
                    inspectorY -= child.fullSize().height() + inspectorHeight + 1;
                    if (inspectorY < 0) inspectorY = 1;
                    if (child instanceof ParentComponent parentComponent) {
                        inspectorX += parentComponent.padding().get().left();
                        inspectorY += parentComponent.padding().get().top();
                    }
                }

                final var nameText = Component.nullToEmpty(child.getClass().getSimpleName() + (child.id() != null ? " '" + child.id() + "'" : ""));
                final var descriptor = Component.literal(child.x() + "," + child.y() + " (" + child.width() + "," + child.height() + ")"
                        + " <" + margins.top() + "," + margins.bottom() + "," + margins.left() + "," + margins.right() + "> ");
                if (child instanceof ParentComponent parentComponent) {
                    var padding = parentComponent.padding().get();
                    descriptor.append(" >" + padding.top() + "," + padding.bottom() + "," + padding.left() + "," + padding.right() + "<");
                }

                int width = Math.max(textRenderer.width(nameText), textRenderer.width(descriptor));
                fill(inspectorX, inspectorY, inspectorX + width + 3, inspectorY + inspectorHeight, 0xA7000000);
                drawRectOutline(inspectorX, inspectorY, width + 3, inspectorHeight, 0xA7000000);

                this.drawString(textRenderer, nameText, inspectorX + 2, inspectorY + 2, 0xFFFFFF, false);
                this.drawString(textRenderer, descriptor, inspectorX + 2, inspectorY + textRenderer.lineHeight + 2, 0xFFFFFF, false);
            }
        }

        RenderSystem.enableDepthTest();
    }

    public static class UtilityScreen extends Screen {

        private static UtilityScreen INSTANCE;

        private UtilityScreen() {
            super(Component.empty());
        }

        public static UtilityScreen get() {
            if (INSTANCE == null) {
                INSTANCE = new UtilityScreen();

                final var client = Minecraft.getInstance();
                INSTANCE.init(
                        client,
                        client.getWindow().getGuiScaledWidth(),
                        client.getWindow().getGuiScaledHeight()
                );
            }

            return INSTANCE;
        }

        static {
            WindowResizeCallback.EVENT.register((client, window) -> {
                if (INSTANCE == null) return;
                INSTANCE.init(client, window.getGuiScaledWidth(), window.getGuiScaledHeight());
            });
        }
    }
}
