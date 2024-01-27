package io.wispforest.owo.ui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.parsing.UIParsing;
import io.wispforest.owo.ui.util.SpriteUtilInvoker;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import org.w3c.dom.Element;

public class SpriteComponent extends BaseComponent {

    protected final TextureAtlasSprite sprite;

    protected SpriteComponent(TextureAtlasSprite sprite) {
        this.sprite = sprite;
    }

    @Override
    protected int determineHorizontalContentSize(Sizing sizing) {
        return this.sprite.contents().width();
    }

    @Override
    protected int determineVerticalContentSize(Sizing sizing) {
        return this.sprite.contents().height();
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        SpriteUtilInvoker.markSpriteActive(this.sprite);
        context.blit(this.x, this.y, 0, this.width, this.height, this.sprite);
    }

    public static SpriteComponent parse(Element element) {
        UIParsing.expectAttributes(element, "atlas", "sprite");

        var atlas = UIParsing.parseIdentifier(element.getAttributeNode("atlas"));
        var sprite = UIParsing.parseIdentifier(element.getAttributeNode("sprite"));

        return Components.sprite(new Material(atlas, sprite));
    }
}
