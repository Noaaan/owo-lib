package io.wispforest.uwu.client;

import io.wispforest.owo.config.ui.ConfigScreen;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.uwu.Uwu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SelectUwuScreenScreen extends BaseOwoScreen<FlowLayout> {

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        this.uiAdapter.rootComponent.surface(Surface.flat(0x77000000))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        this.uiAdapter.rootComponent.child(
                Components.label(Component.literal("Available screens"))
                        .shadow(true)
                        .margins(Insets.bottom(5))
        );

        var panel = Containers.verticalFlow(Sizing.content(), Sizing.content()).<FlowLayout>configure(layout -> {
            layout.gap(6)
                    .padding(Insets.of(5))
                    .surface(Surface.PANEL)
                    .horizontalAlignment(HorizontalAlignment.CENTER);
        });

        panel.child(Components.button(Component.literal("code demo"), button -> this.minecraft.setScreen(new ComponentTestScreen())));
        panel.child(Components.button(Component.literal("xml demo"), button -> this.minecraft.setScreen(new TestParseScreen())));
        panel.child(Components.button(Component.literal("code config"), button -> this.minecraft.setScreen(new TestConfigScreen())));
        panel.child(Components.button(Component.literal("xml config"), button -> this.minecraft.setScreen(ConfigScreen.create(Uwu.CONFIG, null))));
        panel.child(Components.button(Component.literal("optimization test"), button -> this.minecraft.setScreen(new TooManyComponentsScreen())));
        panel.child(Components.button(Component.literal("focus cycle test"), button -> this.minecraft.setScreen(new BaseUIModelScreen<>(FlowLayout.class, new ResourceLocation("uwu", "focus_cycle_test")) {
            @Override
            protected void build(FlowLayout rootComponent) {}
        })));
        panel.child(Components.button(Component.literal("smolnite"), button -> this.minecraft.setScreen(new SmolComponentTestScreen())));
        panel.child(Components.button(Component.literal("sizenite"), button -> this.minecraft.setScreen(new SizingTestScreen())));

        this.uiAdapter.rootComponent.child(panel);
    }
}
