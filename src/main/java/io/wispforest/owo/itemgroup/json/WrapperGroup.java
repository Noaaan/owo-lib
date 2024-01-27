package io.wispforest.owo.itemgroup.json;

import com.mojang.serialization.Lifecycle;
import io.wispforest.owo.itemgroup.Icon;
import io.wispforest.owo.itemgroup.OwoItemGroup;
import io.wispforest.owo.itemgroup.gui.ItemGroupButton;
import io.wispforest.owo.itemgroup.gui.ItemGroupTab;
import io.wispforest.owo.mixin.itemgroup.ItemGroupAccessor;
import io.wispforest.owo.mixin.ui.SimpleRegistryAccessor;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.List;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

/**
 * Used to replace a vanilla or modded item group to add the JSON-defined
 * tabs while keeping the same name, id and icon
 */
@ApiStatus.Internal
public class WrapperGroup extends OwoItemGroup {

    private final CreativeModeTab parent;
    private boolean extension = false;

    @SuppressWarnings("unchecked")
    public WrapperGroup(CreativeModeTab parent, ResourceLocation parentId, List<ItemGroupTab> tabs, List<ItemGroupButton> buttons) {
        super(parentId, owoItemGroup -> {}, () -> Icon.of(parent.getIconItem()), 4, 4, null, null, null, true, false, false);

        int parentRawId = BuiltInRegistries.CREATIVE_MODE_TAB.getId(parent);

        ((SimpleRegistryAccessor<CreativeModeTab>) BuiltInRegistries.CREATIVE_MODE_TAB).owo$getValueToEntry().remove(parent);
        ((SimpleRegistryAccessor<CreativeModeTab>) BuiltInRegistries.CREATIVE_MODE_TAB).owo$getEntryToLifecycle().remove(parent);
        ((MappedRegistry<CreativeModeTab>) BuiltInRegistries.CREATIVE_MODE_TAB).registerMapping(parentRawId, ResourceKey.create(Registries.CREATIVE_MODE_TAB, parentId), this, Lifecycle.stable());

        ((ItemGroupAccessor) this).owo$setDisplayName(parent.getDisplayName());
        ((ItemGroupAccessor) this).owo$setColumn(parent.column());
        ((ItemGroupAccessor) this).owo$setRow(parent.row());

        this.parent = parent;

        this.tabs.addAll(tabs);
        this.buttons.addAll(buttons);
    }

    public void addTabs(Collection<ItemGroupTab> tabs) {
        this.tabs.addAll(tabs);
    }

    public void addButtons(Collection<ItemGroupButton> buttons) {
        this.buttons.addAll(buttons);
    }

    public void markExtension() {
        if (this.extension) return;
        this.extension = true;

        if (this.tabs.get(0) == PLACEHOLDER_TAB) {
            this.tabs.remove(0);
        }

        this.tabs.add(0, new ItemGroupTab(
                Icon.of(this.parent.getIconItem()),
                this.parent.getDisplayName(),
                ((ItemGroupAccessor) this.parent).owo$getEntryCollector()::accept,
                ItemGroupTab.DEFAULT_TEXTURE,
                true
        ));
    }
}
