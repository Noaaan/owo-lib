package io.wispforest.owo.ui.core;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.wispforest.owo.client.OwoClient;
import io.wispforest.owo.ui.parsing.UIModelParsingException;
import io.wispforest.owo.ui.parsing.UIParsing;
import io.wispforest.owo.ui.util.NinePatchTexture;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public interface Surface {

    Surface PANEL = (context, component) -> {
        context.drawPanel(component.x(), component.y(), component.width(), component.height(), false);
    };

    Surface DARK_PANEL = (context, component) -> {
        context.drawPanel(component.x(), component.y(), component.width(), component.height(), true);
    };

    Surface PANEL_INSET = (context, component) -> {
        NinePatchTexture.draw(OwoUIDrawContext.PANEL_INSET_NINE_PATCH_TEXTURE, context, component);
    };

    Surface VANILLA_TRANSLUCENT = (context, component) -> {
        context.drawGradientRect(
                component.x(), component.y(), component.width(), component.height(),
                0xC0101010, 0xC0101010, 0xD0101010, 0xD0101010
        );
    };

    Surface OPTIONS_BACKGROUND = (context, component) -> {
        RenderSystem.setShaderColor(64 / 255f, 64 / 255f, 64 / 255f, 1);
        context.blit(Screen.BACKGROUND_LOCATION, component.x(), component.y(), 0, 0, component.width(), component.height(), 32, 32);
        RenderSystem.setShaderColor(1, 1, 1, 1);
    };

    Surface TOOLTIP = (context, component) -> {
        var buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        TooltipRenderUtil.renderTooltipBackground(context, component.x() + 4, component.y() + 4, component.width() - 8, component.height() - 8, 0);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator.getInstance().end();
    };

    static Surface blur(float quality, float size) {
        return (context, component) -> {
            var buffer = Tesselator.getInstance().getBuilder();
            var matrix = context.pose().last().pose();

            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            buffer.vertex(matrix, component.x(), component.y(), 0).endVertex();
            buffer.vertex(matrix, component.x(), component.y() + component.height(), 0).endVertex();
            buffer.vertex(matrix, component.x() + component.width(), component.y() + component.height(), 0).endVertex();
            buffer.vertex(matrix, component.x() + component.width(), component.y(), 0).endVertex();

            OwoClient.BLUR_PROGRAM.setParameters(16, quality, size);
            OwoClient.BLUR_PROGRAM.use();
            Tesselator.getInstance().end();
        };
    }

    Surface BLANK = (context, component) -> {};

    static Surface flat(int color) {
        return (context, component) -> context.fill(component.x(), component.y(), component.x() + component.width(), component.y() + component.height(), color);
    }

    static Surface outline(int color) {
        return (context, component) -> context.drawRectOutline(component.x(), component.y(), component.width(), component.height(), color);
    }

    static Surface tiled(ResourceLocation texture, int textureWidth, int textureHeight) {
        return (context, component) -> {
            context.blit(texture, component.x(), component.y(), 0, 0, component.width(), component.height(), textureWidth, textureHeight);
        };
    }

    static Surface panelWithInset(int insetWidth) {
        return Surface.PANEL.and((context, component) -> {
            NinePatchTexture.draw(
                    OwoUIDrawContext.PANEL_INSET_NINE_PATCH_TEXTURE,
                    context,
                    component.x() + insetWidth,
                    component.y() + insetWidth,
                    component.width() - insetWidth * 2,
                    component.height() - insetWidth * 2
            );
        });
    }

    void draw(OwoUIDrawContext context, ParentComponent component);

    default Surface and(Surface surface) {
        return (context, component) -> {
            this.draw(context, component);
            surface.draw(context, component);
        };
    }

    static Surface parse(Element surfaceElement) {
        var children = UIParsing.<Element>allChildrenOfType(surfaceElement, Node.ELEMENT_NODE);
        var surface = BLANK;

        for (var child : children) {
            surface = switch (child.getNodeName()) {
                case "panel" -> surface.and(child.getAttribute("dark").equalsIgnoreCase("true")
                        ? DARK_PANEL
                        : PANEL);
                case "tiled" -> {
                    UIParsing.expectAttributes(child, "texture-width", "texture-height");
                    yield surface.and(tiled(
                            UIParsing.parseIdentifier(child),
                            UIParsing.parseUnsignedInt(child.getAttributeNode("texture-width")),
                            UIParsing.parseUnsignedInt(child.getAttributeNode("texture-height")))
                    );
                }
                case "blur" -> {
                    UIParsing.expectAttributes(child, "size", "quality");
                    yield surface.and(blur(
                            UIParsing.parseFloat(child.getAttributeNode("quality")),
                            UIParsing.parseFloat(child.getAttributeNode("size"))
                    ));
                }
                case "panel-with-inset" -> surface.and(panelWithInset(UIParsing.parseUnsignedInt(child)));
                case "options-background" -> surface.and(OPTIONS_BACKGROUND);
                case "vanilla-translucent" -> surface.and(VANILLA_TRANSLUCENT);
                case "panel-inset" -> surface.and(PANEL_INSET);
                case "tooltip" -> surface.and(TOOLTIP);
                case "outline" -> surface.and(outline(Color.parseAndPack(child)));
                case "flat" -> surface.and(flat(Color.parseAndPack(child)));
                default -> throw new UIModelParsingException("Unknown surface type '" + child.getNodeName() + "'");
            };
        }

        return surface;
    }
}
