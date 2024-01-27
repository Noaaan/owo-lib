package io.wispforest.owo.mixin.offline;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;

@Mixin(targets = "net/minecraft/server/PlayerAdvancements$Data")
public interface ProgressMapAccessor {
    @Accessor
    Map<ResourceLocation, AdvancementProgress> getMap();
}
