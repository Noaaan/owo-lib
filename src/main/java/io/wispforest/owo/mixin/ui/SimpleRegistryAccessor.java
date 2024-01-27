package io.wispforest.owo.mixin.ui;

import com.mojang.serialization.Lifecycle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;

@Mixin(MappedRegistry.class)
public interface SimpleRegistryAccessor<T> {

    @Accessor("byValue")
    Map<T, Holder.Reference<T>> owo$getValueToEntry();

    @Accessor("lifecycles")
    Map<T, Lifecycle> owo$getEntryToLifecycle();
}
