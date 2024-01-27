package io.wispforest.owo.mixin.recipe_remainders;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import io.wispforest.owo.util.RecipeRemainderStorage;
import net.minecraft.Util;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.HashMap;
import java.util.Optional;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {

    @Inject(method = "fromJson", at = @At(value = "RETURN"))
    private static void deserializeRecipeSpecificRemainders(ResourceLocation id, JsonObject json, CallbackInfoReturnable<Recipe<?>> cir) {
        if (!json.has("owo:remainders")) return;

        var remainders = new HashMap<Item, ItemStack>();
        for (var remainderEntry : json.getAsJsonObject("owo:remainders").entrySet()) {
            var item = GsonHelper.convertToItem(new JsonPrimitive(remainderEntry.getKey()), remainderEntry.getKey());

            if (remainderEntry.getValue().isJsonObject()) {
                var remainderStack = Util.getOrThrow(ItemStack.ITEM_WITH_COUNT_CODEC.parse(JsonOps.INSTANCE, remainderEntry.getValue().getAsJsonObject()), JsonParseException::new);
                remainders.put(item.value(), remainderStack);
            } else {
                var remainderItem = GsonHelper.convertToItem(remainderEntry.getValue(), "item");
                remainders.put(item.value(), new ItemStack(remainderItem));
            }
        }

        if (remainders.isEmpty()) return;
        RecipeRemainderStorage.store(id, remainders);
    }

    @Inject(method = "getRemainingItemsFor", at = @At(value = "RETURN", ordinal = 0), locals = LocalCapture.CAPTURE_FAILHARD)
    private <I extends Container, R extends Recipe<I>> void addRecipeSpecificRemainders(RecipeType<R> type, I inventory, Level world, CallbackInfoReturnable<NonNullList<ItemStack>> cir, Optional<RecipeHolder<R>> optional) {
        if (optional.isEmpty() || !RecipeRemainderStorage.has(optional.get().id())) return;

        var remainders = cir.getReturnValue();
        var owoRemainders = RecipeRemainderStorage.get(optional.get().id());

        for (int i = 0; i < remainders.size(); ++i) {
            var item = inventory.getItem(i).getItem();
            if (!owoRemainders.containsKey(item)) continue;

            remainders.set(i, owoRemainders.get(item).copy());
        }
    }
}
