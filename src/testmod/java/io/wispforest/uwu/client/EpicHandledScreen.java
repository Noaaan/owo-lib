package io.wispforest.uwu.client;

import io.wispforest.owo.mixin.ui.SlotAccessor;
import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.uwu.EpicScreenHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class EpicHandledScreen extends BaseOwoHandledScreen<FlowLayout, EpicScreenHandler> {
    private LabelComponent numberLabel;


    public EpicHandledScreen(EpicScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        var frogeNbt = new CompoundTag();
        frogeNbt.putString("variant", "delightful:froge");

        var selectBox = Components.textBox(Sizing.fixed(40));
        selectBox.setFilter(s -> s.matches("\\d*"));

        rootComponent.child(
                Components.texture(new ResourceLocation("textures/gui/container/shulker_box.png"), 0, 0, 176, 166)
        ).child(
                Containers.draggable(
                        Sizing.content(), Sizing.content(),
                        Containers.verticalFlow(Sizing.content(), Sizing.content())
                                .child(Components.label(Component.literal("froge :)"))
                                        .horizontalTextAlignment(HorizontalAlignment.CENTER)
                                        .positioning(Positioning.absolute(0, -9))
                                        .horizontalSizing(Sizing.fixed(100)))
                                .child(Components.entity(Sizing.fixed(100), EntityType.FROG, frogeNbt).scale(.75f).allowMouseRotation(true).tooltip(Component.literal(":)")))
                                .child(Containers.horizontalFlow(Sizing.fixed(100), Sizing.content())
                                        .child(Components.button(Component.nullToEmpty("✔"), (ButtonComponent button) -> {
                                            this.enableSlot(Integer.parseInt(selectBox.getValue()));
                                        }).tooltip(Component.literal("Enable")))
                                        .child(selectBox.margins(Insets.horizontal(3)).tooltip(Component.literal("Slot Index")))
                                        .child(Components.button(Component.nullToEmpty("❌"), (ButtonComponent button) -> {
                                            this.disableSlot(Integer.parseInt(selectBox.getValue()));
                                        }).tooltip(Component.literal("Disable"))).verticalAlignment(VerticalAlignment.CENTER).horizontalAlignment(HorizontalAlignment.CENTER))
                                .allowOverflow(true)
                ).surface(Surface.DARK_PANEL).padding(Insets.of(5)).allowOverflow(true).zIndex(500).positioning(Positioning.absolute(100, 100))
        ).child(
                Containers.verticalScroll(Sizing.content(), Sizing.fill(50), Containers.verticalFlow(Sizing.content(), Sizing.content())
                        .child(this.slotAsComponent(0).tooltip(Component.nullToEmpty("bruh")))
                        .child(Components.box(Sizing.fixed(50), Sizing.fixed(35)).startColor(Color.RED).endColor(Color.BLUE).fill(true).tooltip(Component.literal("very very long tooltip")))
                        .child(this.slotAsComponent(1))
                        .child(Components.box(Sizing.fixed(50), Sizing.fixed(35)).startColor(Color.BLUE).endColor(Color.RED).fill(true))
                        .child(this.slotAsComponent(2))
                        .child(Components.box(Sizing.fixed(50), Sizing.fixed(35)).startColor(Color.RED).endColor(Color.BLUE).fill(true))
                        .child(this.slotAsComponent(3))
                        .child(Components.box(Sizing.fixed(50), Sizing.fixed(35)).startColor(Color.BLUE).endColor(Color.RED).fill(true))
                ).positioning(Positioning.relative(75, 50)).surface(Surface.outline(0x77000000)).padding(Insets.of(1))
        ).surface(Surface.VANILLA_TRANSLUCENT).verticalAlignment(VerticalAlignment.CENTER).horizontalAlignment(HorizontalAlignment.CENTER);

        rootComponent.child(
                (numberLabel = Components.label(Component.literal(this.menu.epicNumber.get())))
                        .positioning(Positioning.absolute(0, 0))
        );

        this.menu.epicNumber.observe(value -> numberLabel.text(Component.literal(value)));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (Screen.hasAltDown() && this.hoveredSlot != null) {
            return false;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            this.uiAdapter.rootComponent.child(Containers.overlay(Components.label(Component.literal("a"))));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (Screen.hasAltDown() && this.hoveredSlot != null) {
            var accessor = ((SlotAccessor) this.hoveredSlot);
            accessor.owo$setX((int) Math.round(this.hoveredSlot.x + deltaX));
            accessor.owo$setY((int) Math.round(this.hoveredSlot.y + deltaY));
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
}
