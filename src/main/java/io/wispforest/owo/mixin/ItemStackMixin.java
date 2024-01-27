package io.wispforest.owo.mixin;

import io.wispforest.owo.serialization.SerializationAttribute;
import io.wispforest.owo.serialization.util.MapCarrier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import io.wispforest.owo.serialization.endec.KeyedEndec;
import io.wispforest.owo.serialization.format.nbt.NbtDeserializer;
import io.wispforest.owo.serialization.format.nbt.NbtSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements MapCarrier {

    @Shadow
    private @Nullable CompoundTag tag;

    @Shadow
    public abstract CompoundTag getOrCreateTag();

    @Override
    public <T> T getWithErrors(@NotNull KeyedEndec<T> key) {
        if (!this.has(key)) return key.defaultValue();
        return key.endec().decodeFully(e -> NbtDeserializer.of(e).withAttributes(SerializationAttribute.HUMAN_READABLE), this.tag.get(key.key()));
    }

    @Override
    public <T> void put(@NotNull KeyedEndec<T> key, @NotNull T value) {
        this.getOrCreateTag().put(key.key(), key.endec().encodeFully(() -> NbtSerializer.of().withAttributes(SerializationAttribute.HUMAN_READABLE), value));
    }

    @Override
    public <T> void delete(@NotNull KeyedEndec<T> key) {
        if (this.tag == null) return;
        this.tag.remove(key.key());
    }

    @Override
    public <T> boolean has(@NotNull KeyedEndec<T> key) {
        return this.tag != null && this.tag.contains(key.key());
    }
}
