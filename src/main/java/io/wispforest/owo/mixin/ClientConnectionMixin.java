package io.wispforest.owo.mixin;

import io.wispforest.owo.network.OwoClientConnectionExtension;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Collections;
import java.util.Set;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;

@Mixin(Connection.class)
public class ClientConnectionMixin implements OwoClientConnectionExtension {
    private Set<ResourceLocation> channels = Collections.emptySet();

    @Override
    public void owo$setChannelSet(Set<ResourceLocation> channels) {
        this.channels = channels;
    }

    @Override
    public Set<ResourceLocation> owo$getChannelSet() {
        return this.channels;
    }
}
