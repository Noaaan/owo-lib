package io.wispforest.owo.serialization.util;

import com.mojang.serialization.Codec;
import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.serialization.SerializationAttribute;
import io.wispforest.owo.serialization.StructEndec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

public abstract class EndecRecipeSerializer<R extends Recipe<?>> implements RecipeSerializer<R> {

    private final StructEndec<R> endec;
    private final Endec<R> networkEndec;
    private final Codec<R> codec;

    protected EndecRecipeSerializer(StructEndec<R> endec, Endec<R> networkEndec) {
        this.endec = endec;
        this.networkEndec = networkEndec;
        this.codec = this.endec.mapCodec(SerializationAttribute.HUMAN_READABLE).codec();
    }

    protected EndecRecipeSerializer(StructEndec<R> endec) {
        this(endec, endec);
    }

    @Override
    public Codec<R> codec() {
        return this.codec;
    }

    @Override
    public R fromNetwork(FriendlyByteBuf buf) {
        return buf.read(this.networkEndec);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, R recipe) {
        buf.write(this.networkEndec, recipe);
    }
}
