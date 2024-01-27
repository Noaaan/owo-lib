package io.wispforest.owo.network;

import org.jetbrains.annotations.ApiStatus;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;

@ApiStatus.Internal
public interface OwoClientConnectionExtension {
    void owo$setChannelSet(Set<ResourceLocation> channels);

    Set<ResourceLocation> owo$getChannelSet();
}
