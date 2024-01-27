package io.wispforest.owo.itemgroup.gui;

import io.wispforest.owo.itemgroup.Icon;
import io.wispforest.owo.itemgroup.OwoItemGroup;
import io.wispforest.owo.itemgroup.OwoItemSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

/**
 * Represents a tab inside an {@link OwoItemGroup} that contains all items in the
 * passed {@code contentTag}. If you want to use {@link OwoItemSettings#tab(int)} to
 * define the contents, use {@code null} as the tag
 */
public record ItemGroupTab(
        Icon icon,
        Component name,
        ContentSupplier contentSupplier,
        ResourceLocation texture,
        boolean primary
) implements OwoItemGroup.ButtonDefinition {

    public static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation("owo", "textures/gui/tabs.png");

    @Override
    public Component tooltip() {
        return this.name;
    }

    @FunctionalInterface
    public interface ContentSupplier {
        void addItems(CreativeModeTab.ItemDisplayParameters context, CreativeModeTab.Output entries);
    }
}
