package io.wispforest.owo.mixin;

import io.wispforest.owo.util.RegistryAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceLocation;

@Mixin(MappedRegistry.class)
public class SimpleRegistryMixin<T> implements RegistryAccess.AccessibleRegistry<T> {

    @Shadow
    @Final
    private Map<ResourceLocation, Holder.Reference<T>> byLocation;

    @Shadow
    @Final
    private Map<T, Holder.Reference<T>> byValue;

    @Override
    public @Nullable Holder<T> getEntry(ResourceLocation id) {
        return this.byLocation.get(id);
    }

    @Override
    public @Nullable Holder<T> getEntry(T value) {
        return this.byValue.get(value);
    }
}
