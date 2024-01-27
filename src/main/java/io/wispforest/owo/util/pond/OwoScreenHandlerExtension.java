package io.wispforest.owo.util.pond;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public interface OwoScreenHandlerExtension {
    void owo$attachToPlayer(Player player);

    void owo$readPropertySync(FriendlyByteBuf buf);

    void owo$handlePacket(FriendlyByteBuf buf, boolean clientbound);
}
