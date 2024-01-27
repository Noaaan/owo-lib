package io.wispforest.owo.mixin.ui.access;

import net.minecraft.client.gui.components.Checkbox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Checkbox.class)
public interface CheckboxWidgetAccessor {
    @Accessor("selected")
    void owo$setChecked(boolean checked);
}
