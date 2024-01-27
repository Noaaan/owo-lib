package io.wispforest.uwu.recipe;

import com.mojang.serialization.Codec;
import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.serialization.SerializationAttribute;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class EndecRecipeSerializer<T extends Recipe<?>> implements RecipeSerializer<T> {

    public final Endec<T> endec;
    public final Codec<T> codec;

    public EndecRecipeSerializer(Endec<T> endec){
        this.codec = endec.codec(SerializationAttribute.HUMAN_READABLE);
        this.endec = endec;
    }

    @Override
    public Codec<T> codec() {
        return this.codec;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, T recipe) {
        buf.write(this.endec, recipe);
    }

    @Override
    public T fromNetwork(FriendlyByteBuf buf) {
        return buf.read(this.endec);
    }
}
