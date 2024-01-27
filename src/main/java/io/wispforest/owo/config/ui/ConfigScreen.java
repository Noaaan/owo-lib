package io.wispforest.owo.config.ui;




import io.wispforest.owo.Owo;
import io.wispforest.owo.config.ConfigWrapper;
import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.Option.Key;
import io.wispforest.owo.config.annotation.ExcludeFromScreen;
import io.wispforest.owo.config.annotation.Expanded;
import io.wispforest.owo.config.annotation.RestartRequired;
import io.wispforest.owo.config.annotation.SectionHeader;
import io.wispforest.owo.config.ui.OptionComponentFactory.Result;
import io.wispforest.owo.config.ui.component.*;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.parsing.UIParsing;
import io.wispforest.owo.ui.util.UISounds;
import io.wispforest.owo.util.NumberReflection;
import io.wispforest.owo.util.ReflectionUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

/**
 * A screen which generates components for each option in the
 * provided config. The general structure of the screen is determined
 * by the XML config model it uses - the default one is located at
 * {@code assets/owo/owo_ui/config.xml}. Changing which model is used
 * via {@link #createWithCustomModel(ResourceLocation, ConfigWrapper, Screen)}
 * can often be enough to visually customize the generated screen - should
 * you need custom functionality however, extending this class is usually
 * your best bet
 *
 * @see io.wispforest.owo.config.annotation.Modmenu
 * @see ConfigWrapper
 */
public class ConfigScreen extends BaseUIModelScreen<FlowLayout> {

    public static final ResourceLocation DEFAULT_MODEL_ID = new ResourceLocation("owo", "config");

    private static final Map<String, Function<Screen, ? extends ConfigScreen>> CONFIG_SCREEN_PROVIDERS = new HashMap<>();

    private static final Map<Predicate<Option<?>>, OptionComponentFactory<?>> DEFAULT_FACTORIES = new HashMap<>();
    /**
     * A set of extra option factories - add to this if you want to override
     * some default factories or add extra ones for specific config options
     * the standard ones don't support
     */
    protected final Map<Predicate<Option<?>>, OptionComponentFactory<?>> extraFactories = new HashMap<>();

    protected final Screen parent;
    protected final ConfigWrapper<?> config;
    @SuppressWarnings("rawtypes") protected final Map<Option, OptionValueProvider> options = new HashMap<>();

    protected String lastSearchFieldText = "";
    protected @Nullable SearchMatches currentMatches = null;
    protected int currentMatchIndex = 0;

    protected ConfigScreen(ResourceLocation modelId, ConfigWrapper<?> config, @Nullable Screen parent) {
        super(FlowLayout.class, DataSource.asset(modelId));
        this.parent = parent;
        this.config = config;
    }

    /**
     * Create a config screen with the default model ({@code owo:config})
     *
     * @param config The config to create a screen for
     * @param parent The parent screen to return to
     *               when the created screen is closed
     */
    public static ConfigScreen create(ConfigWrapper<?> config, @Nullable Screen parent) {
        return new ConfigScreen(DEFAULT_MODEL_ID, config, parent);
    }

    /**
     * Create a config screen with a custom model
     * located in your mod's assets
     *
     * @param modelId The ID of the model to use
     * @param config  The config to create a screen for
     * @param parent  The parent screen to return to
     *                when the created screen is closed
     */
    public static ConfigScreen createWithCustomModel(ResourceLocation modelId, ConfigWrapper<?> config, @Nullable Screen parent) {
        return new ConfigScreen(modelId, config, parent);
    }

    /**
     * Register the given config screen provider. This is primarily
     * used for making your config available in ModMenu and to the
     * {@code /owo-config} command, although other places my use it as well
     *
     * @param modId    The mod id for which to supply a config screen
     * @param supplier The supplier to register - this gets the parent screen
     *                 as argument
     * @throws IllegalArgumentException If a config screen provider is
     *                                  already registered for the given mod id
     */
    public static <S extends ConfigScreen> void registerProvider(String modId, Function<Screen, S> supplier) {
        if (CONFIG_SCREEN_PROVIDERS.put(modId, supplier) != null) {
            throw new IllegalArgumentException("Tried to register config screen provider for mod id " + modId + " twice");
        }
    }

    /**
     * Get the config screen provider associated with
     * the given mod id
     *
     * @return The associated config screen provider, or {@code null} if
     * none is registered
     */
    public static @Nullable Function<Screen, ? extends ConfigScreen> getProvider(String modId) {
        return CONFIG_SCREEN_PROVIDERS.get(modId);
    }

    public static void forEachProvider(BiConsumer<String, Function<Screen, ? extends ConfigScreen>> action) {
        CONFIG_SCREEN_PROVIDERS.forEach(action);
    }

    @Override
    @SuppressWarnings({"ConstantConditions", "unchecked"})
    protected void build(FlowLayout rootComponent) {
        this.options.clear();

        rootComponent.childById(LabelComponent.class, "title").text(Component.translatable("text.config." + this.config.name() + ".title"));
        if (this.minecraft.level == null) {
            rootComponent.surface(Surface.OPTIONS_BACKGROUND);
        }

        rootComponent.childById(ButtonComponent.class, "done-button").onPress(button -> this.onClose());
        rootComponent.childById(ButtonComponent.class, "reload-button").onPress(button -> {
            this.config.load();
            this.uiAdapter = null;
            this.rebuildWidgets();

            // TODO check if any options changed and warn
        });

        var optionPanel = rootComponent.childById(FlowLayout.class, "option-panel");
        var sections = new LinkedHashMap<io.wispforest.owo.ui.core.Component, Component>();

        var containers = new HashMap<Option.Key, FlowLayout>();
        containers.put(Option.Key.ROOT, optionPanel);

        rootComponent.childById(TextBoxComponent.class, "search-field").<TextBoxComponent>configure(searchField -> {
            var matchIndicator = rootComponent.childById(LabelComponent.class, "search-match-indicator");
            var optionScroll = rootComponent.childById(ScrollContainer.class, "option-panel-scroll");

            var searchHint = I18n.get("text.owo.config.search");
            searchField.setSuggestion(searchHint);
            searchField.onChanged().subscribe(s -> {
                searchField.setSuggestion(s.isEmpty() ? searchHint : "");
                if (!s.equals(this.lastSearchFieldText)) {
                    searchField.setTextColor(TextBoxComponent.DEFAULT_TEXT_COLOR);
                    matchIndicator.text(Component.empty());
                }
            });

            searchField.keyPress().subscribe((keyCode, scanCode, modifiers) -> {
                if (keyCode != GLFW.GLFW_KEY_ENTER) return false;

                var query = searchField.getValue().toLowerCase(Locale.ROOT);
                if (query.isBlank()) return false;

                if (this.currentMatches != null && this.currentMatches.query.equals(query)) {
                    if (this.currentMatches.matches().isEmpty()) {
                        this.currentMatchIndex = -1;
                    } else {
                        this.currentMatchIndex = (this.currentMatchIndex + 1) % this.currentMatches.matches.size();
                    }
                } else {
                    var splitQuery = query.split(" ");

                    this.currentMatchIndex = 0;
                    this.currentMatches = new SearchMatches(query, this.collectSearchAnchors(optionScroll)
                            .stream()
                            .filter(anchor -> Arrays.stream(splitQuery).allMatch(anchor.currentSearchText()::contains))
                            .toList());
                }

                if (this.currentMatches.matches.isEmpty()) {
                    matchIndicator.text(Component.translatable("text.owo.config.search.no_matches"));
                    searchField.setTextColor(0xEB1D36);
                } else {
                    matchIndicator.text(Component.translatable("text.owo.config.search.matches", this.currentMatchIndex + 1, this.currentMatches.matches.size()));
                    searchField.setTextColor(0x28FFBF);

                    var selectedMatch = this.currentMatches.matches.get(this.currentMatchIndex);
                    var anchorFrame = selectedMatch.anchorFrame();

                    // we specifically build the path backwards, so we can then iterate
                    // it root -> key, otherwise we could potentially be manipulating
                    // unmounted components which is absolutely not desirable
                    var pathToRoot = new ArrayDeque<Option.Key>();
                    var key = selectedMatch.key();
                    while (!key.isRoot()) {
                        pathToRoot.push(key);
                        key = key.parent();
                    }

                    while (!pathToRoot.isEmpty()) {
                        if (containers.get(pathToRoot.pop()) instanceof CollapsibleContainer collapsible && !collapsible.expanded()) {
                            collapsible.toggleExpansion();
                        }
                    }

                    // in the same vein, the component is mounted after the layout is fully
                    // restored, as we would otherwise be mounting onto a partially-built subtree
                    if (anchorFrame instanceof FlowLayout flow) {
                        flow.child(0, selectedMatch.configure(new SearchHighlighterComponent()));
                    }

                    if (anchorFrame.y() < optionScroll.y() || anchorFrame.y() + anchorFrame.height() > optionScroll.y() + optionScroll.height()) {
                        optionScroll.scrollTo(selectedMatch.anchorFrame());
                    }
                }

                return true;
            });
        });

        this.config.forEachOption(option -> {
            if (option.backingField().hasAnnotation(ExcludeFromScreen.class)) return;

            var parentKey = option.key().parent();
            if (!parentKey.isRoot() && this.config.fieldForKey(parentKey).isAnnotationPresent(ExcludeFromScreen.class)) return;

            var factory = this.factoryForOption(option);
            if (factory == null) {
                Owo.LOGGER.warn("Could not create UI component for config option {}", option);
                return;
            }

            var result = factory.make(this.model, option);
            this.options.put(option, result.optionProvider());

            var expanded = !parentKey.isRoot() && this.config.fieldForKey(parentKey).isAnnotationPresent(Expanded.class);
            var container = containers.getOrDefault(
                    parentKey,
                    Containers.collapsible(
                            Sizing.fill(100), Sizing.content(),
                            Component.translatable("text.config." + this.config.name() + ".category." + parentKey.asString()),
                            expanded
                    ).<CollapsibleContainer>configure(nestedContainer -> {
                        final var categoryKey = "text.config." + this.config.name() + ".category." + parentKey.asString();
                        if (I18n.exists(categoryKey + ".tooltip")) {
                            nestedContainer.titleLayout().tooltip(Component.translatable(categoryKey + ".tooltip"));
                        }

                        nestedContainer.titleLayout().child(new SearchAnchorComponent(
                                nestedContainer.titleLayout(),
                                option.key(),
                                () -> I18n.get(categoryKey)
                        ).highlightConfigurator(highlight ->
                                highlight.positioning(Positioning.absolute(-5, -5))
                                        .verticalSizing(Sizing.fixed(19))
                        ));
                    })
            );

            if (!containers.containsKey(parentKey) && containers.containsKey(parentKey.parent())) {
                if (this.config.fieldForKey(parentKey).isAnnotationPresent(SectionHeader.class)) {
                    this.appendSection(sections, this.config.fieldForKey(parentKey), containers.get(parentKey.parent()));
                }

                containers.put(parentKey, container);
                containers.get(parentKey.parent()).child(container);
            }

            if (option.detached()) {
                result.baseComponent().tooltip(
                        this.minecraft.font.split(Component.translatable("text.owo.config.managed_by_server"), Integer.MAX_VALUE)
                                .stream().map(ClientTooltipComponent::create).toList()
                );
            } else {
                var tooltipText = new ArrayList<FormattedCharSequence>();
                var tooltipTranslationKey = option.translationKey() + ".tooltip";

                if (I18n.exists(tooltipTranslationKey)) {
                    tooltipText.addAll(this.minecraft.font.split(Component.translatable(tooltipTranslationKey), Integer.MAX_VALUE));
                }

                if (option.backingField().hasAnnotation(RestartRequired.class)) {
                    tooltipText.add(Component.translatable("text.owo.config.applies_after_restart").getVisualOrderText());
                }

                if (!tooltipText.isEmpty()) {
                    result.baseComponent().tooltip(tooltipText.stream().map(ClientTooltipComponent::create).toList());
                }
            }

            if (option.backingField().hasAnnotation(SectionHeader.class)) {
                this.appendSection(sections, option.backingField().field(), container);
            }

            container.child(result.baseComponent());
        });

        if (!sections.isEmpty()) {
            var panelContainer = rootComponent.childById(FlowLayout.class, "option-panel-container");
            var panelScroll = rootComponent.childById(ScrollContainer.class, "option-panel-scroll");
            panelScroll.margins(Insets.right(10));

            var buttonPanel = this.model.expandTemplate(FlowLayout.class, "section-buttons", Map.of());
            sections.forEach((component, text) -> {
                var hoveredText = text.copy().withStyle(ChatFormatting.YELLOW);

                final var label = Components.label(text);
                label.cursorStyle(CursorStyle.HAND).margins(Insets.of(2));

                label.mouseEnter().subscribe(() -> label.text(hoveredText));
                label.mouseLeave().subscribe(() -> label.text(text));

                label.mouseDown().subscribe((mouseX, mouseY, button) -> {
                    panelScroll.scrollTo(component);
                    UISounds.playInteractionSound();
                    return true;
                });

                buttonPanel.child(label);
            });

            var closeButton = Components.label(Component.literal("<").withStyle(ChatFormatting.BOLD));
            closeButton.positioning(Positioning.relative(100, 50)).cursorStyle(CursorStyle.HAND).margins(Insets.right(2));

            panelContainer.child(closeButton);
            panelContainer.mouseDown().subscribe((mouseX, mouseY, button) -> {
                if (mouseX < panelContainer.width() - 10) return false;

                if (buttonPanel.horizontalSizing().animation() == null) {
                    buttonPanel.horizontalSizing().animate(350, Easing.CUBIC, Sizing.content());
                }

                buttonPanel.horizontalSizing().animation().reverse();
                closeButton.text(Component.literal(closeButton.text().getString().equals(">") ? "<" : ">").withStyle(ChatFormatting.BOLD));

                UISounds.playInteractionSound();
                return true;
            });

            rootComponent.childById(FlowLayout.class, "main-panel").child(buttonPanel);
        }
    }

    protected void appendSection(Map<io.wispforest.owo.ui.core.Component, Component> sections, Field field, FlowLayout container) {
        var translationKey = "text.config." + this.config.name() + ".section."
                + field.getAnnotation(SectionHeader.class).value();

        final var header = this.model.expandTemplate(FlowLayout.class, "section-header", Map.of());
        header.childById(LabelComponent.class, "header").<LabelComponent>configure(label -> {
            label.text(Component.translatable(translationKey).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
            header.child(new SearchAnchorComponent(header, Option.Key.ROOT, () -> label.text().getString()));
        });

        sections.put(header, Component.translatable(translationKey));

        container.child(header);
    }

    protected List<SearchAnchorComponent> collectSearchAnchors(ParentComponent root) {
        var discovered = new ArrayList<SearchAnchorComponent>();
        var candidates = new ArrayDeque<>(root.children());

        while (!candidates.isEmpty()) {
            var candidate = candidates.poll();
            if (candidate instanceof CollapsibleContainer collapsible) {
                candidates.addAll(collapsible.children());
                if (!collapsible.expanded()) candidates.addAll(collapsible.collapsibleChildren());
            } else if (candidate instanceof ParentComponent parentComponent) {
                candidates.addAll(parentComponent.children());
            } else if (candidate instanceof SearchAnchorComponent anchor) {
                discovered.add(anchor);
            }
        }

        return discovered;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_F && ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0)) {
            this.uiAdapter.rootComponent.focusHandler().focus(
                    this.uiAdapter.rootComponent.childById(io.wispforest.owo.ui.core.Component.class, "search-field"),
                    io.wispforest.owo.ui.core.Component.FocusSource.MOUSE_CLICK
            );
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onClose() {
        var shouldRestart = new MutableBoolean();
        this.options.forEach((option, component) -> {
            if (!option.backingField().hasAnnotation(RestartRequired.class)) return;
            if (Objects.equals(option.value(), component.parsedValue())) return;

            shouldRestart.setTrue();
        });

        this.minecraft.setScreen(shouldRestart.booleanValue() ? new RestartRequiredScreen(this.parent) : this.parent);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void removed() {
        this.options.forEach((option, component) -> {
            if (!component.isValid()) return;
            option.set(component.parsedValue());
        });
        super.removed();
    }

    @SuppressWarnings("rawtypes")
    protected @Nullable OptionComponentFactory factoryForOption(Option<?> option) {
        for (var predicate : this.extraFactories.keySet()) {
            if (!predicate.test(option)) continue;
            return this.extraFactories.get(predicate);
        }

        for (var predicate : DEFAULT_FACTORIES.keySet()) {
            if (!predicate.test(option)) continue;
            return DEFAULT_FACTORIES.get(predicate);
        }

        return null;
    }

    static {
        DEFAULT_FACTORIES.put(option -> NumberReflection.isNumberType(option.clazz()), OptionComponentFactory.NUMBER);
        DEFAULT_FACTORIES.put(option -> option.clazz() == String.class, OptionComponentFactory.STRING);
        DEFAULT_FACTORIES.put(option -> option.clazz() == Boolean.class || option.clazz() == boolean.class, OptionComponentFactory.BOOLEAN);
        DEFAULT_FACTORIES.put(option -> option.clazz() == ResourceLocation.class, OptionComponentFactory.IDENTIFIER);
        DEFAULT_FACTORIES.put(option -> option.clazz() == Color.class, OptionComponentFactory.COLOR);
        DEFAULT_FACTORIES.put(option -> isStringOrNumberList(option.backingField().field()), OptionComponentFactory.LIST);
        DEFAULT_FACTORIES.put(option -> option.clazz().isEnum(), OptionComponentFactory.ENUM);

        UIParsing.registerFactory("config-slider", element -> new ConfigSlider());
        UIParsing.registerFactory("config-toggle-button", element -> new ConfigToggleButton());
        UIParsing.registerFactory("config-enum-button", element -> new ConfigEnumButton());
        UIParsing.registerFactory("config-text-box", element -> new ConfigTextBox());
    }

    private record SearchMatches(String query, List<SearchAnchorComponent> matches) {}

    public static class SearchHighlighterComponent extends BaseComponent {

        private final Color startColor = Color.ofArgb(0x008d9be0);
        private final Color endColor = Color.ofArgb(0x4c8d9be0);

        private float age = 0;

        public SearchHighlighterComponent() {
            this.positioning(Positioning.absolute(0, 0));
            this.sizing(Sizing.fill(100), Sizing.fill(100));
        }

        @Override
        public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
            final var mainColor = startColor.interpolate(endColor, (float) Math.sin(age / 25 * Math.PI)).argb();

            int segmentWidth = (int) (this.width * .3f);
            int baseX = (int) ((this.x - segmentWidth) + (Easing.CUBIC.apply(this.age / 25)) * (this.width + segmentWidth * 2));

            context.drawGradientRect(
                    baseX - segmentWidth, this.y,
                    segmentWidth, this.height,
                    0, mainColor,
                    mainColor, 0
            );
            context.drawGradientRect(
                    baseX, this.y,
                    segmentWidth, this.height,
                    mainColor, 0,
                    0, mainColor
            );
        }

        @Override
        public void update(float delta, int mouseX, int mouseY) {
            super.update(delta, mouseX, mouseY);
            if ((this.age += delta) > 25) {
                this.parent.queue(() -> this.parent.removeChild(this));
            }
        }
    }

    private static boolean isStringOrNumberList(Field field) {
        if (field.getType() != List.class) return false;

        var listType = ReflectionUtils.getTypeArgument(field.getGenericType(), 0);
        if (listType == null) return false;

        return String.class == listType || NumberReflection.isNumberType(listType);
    }
}
