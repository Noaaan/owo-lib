package io.wispforest.uwu.rei;

import io.wispforest.owo.compat.rei.ReiUIAdapter;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.core.*;
import io.wispforest.uwu.items.UwuItems;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import java.util.Collections;
import java.util.List;

public class UiCategory implements DisplayCategory<Display> {

    public static CategoryIdentifier<UiDisplay> ID = CategoryIdentifier.of(new ResourceLocation("owo", "ui"));

    @Override
    public List<Widget> setupDisplay(Display display, Rectangle bounds) {
        var adapter = new ReiUIAdapter<>(bounds, Containers::verticalFlow);
        var root = adapter.rootComponent();

        root.horizontalAlignment(HorizontalAlignment.CENTER)
                .surface(Surface.DARK_PANEL)
                .padding(Insets.of(8));

        var inner = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        inner.horizontalAlignment(HorizontalAlignment.CENTER).surface(Surface.flat(0xFF00FFAF));

        inner.child(Components.label(Component.nullToEmpty("A demonstration\ninside REI"))
                .color(Color.BLACK)
                .positioning(Positioning.absolute(3, 3))
        );

        var animation = inner.horizontalSizing().animate(250, Easing.QUADRATIC, Sizing.fill(65));
        inner.child(Components.button(Component.nullToEmpty("shrink"), (ButtonComponent button) -> animation.forwards())
                .margins(Insets.vertical(25))
                .horizontalSizing(Sizing.fixed(60)));
        inner.child(Components.button(Component.nullToEmpty("grow"), (ButtonComponent button) -> animation.backwards())
                .margins(Insets.vertical(25))
                .horizontalSizing(Sizing.fixed(60)));

        inner.child(adapter.wrap(Widgets.createSlot(new Point(0, 0)).entry(EntryStacks.of(Items.ECHO_SHARD))));

        root.child(Containers.verticalScroll(Sizing.content(), Sizing.fill(100), inner));

        adapter.prepare();
        return List.of(adapter);
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(Items.ECHO_SHARD);
    }

    @Override
    public Component getTitle() {
        return Component.nullToEmpty("yes its gui very epic");
    }

    @Override
    public CategoryIdentifier<? extends Display> getCategoryIdentifier() {
        return ID;
    }

    public static class UiDisplay implements Display {

        @Override
        public List<EntryIngredient> getInputEntries() {
            return List.of(EntryIngredients.of(UwuItems.SCREEN_SHARD));
        }

        @Override
        public List<EntryIngredient> getOutputEntries() {
            return Collections.emptyList();
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return ID;
        }
    }
}
