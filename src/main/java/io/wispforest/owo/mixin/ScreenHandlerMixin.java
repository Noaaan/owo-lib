package io.wispforest.owo.mixin;

import io.wispforest.owo.client.screens.OwoScreenHandler;
import io.wispforest.owo.client.screens.ScreenInternals;
import io.wispforest.owo.client.screens.ScreenhandlerMessageData;
import io.wispforest.owo.client.screens.SyncedProperty;
import io.wispforest.owo.network.NetworkException;
import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.util.pond.OwoScreenHandlerExtension;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(AbstractContainerMenu.class)
public abstract class ScreenHandlerMixin implements OwoScreenHandler, OwoScreenHandlerExtension {

    @Shadow private boolean suppressRemoteUpdates;

    private final List<SyncedProperty<?>> owo$properties = new ArrayList<>();

    private final Map<Class<?>, ScreenhandlerMessageData<?>> owo$messages = new LinkedHashMap<>();
    private final List<ScreenhandlerMessageData<?>> owo$clientboundMessages = new ArrayList<>();
    private final List<ScreenhandlerMessageData<?>> owo$serverboundMessages = new ArrayList<>();

    private Player owo$player = null;

    @Override
    public void owo$attachToPlayer(Player player) {
        this.owo$player = player;
    }

    @Override
    public Player player() {
        return this.owo$player;
    }

    @Override
    public <R extends Record> void addServerboundMessage(Class<R> messageClass, Endec<R> endec, Consumer<R> handler) {
        int id = this.owo$serverboundMessages.size();

        var messageData = new ScreenhandlerMessageData<>(id, false, endec, handler);
        this.owo$serverboundMessages.add(messageData);

        if (this.owo$messages.put(messageClass, messageData) != null) {
            throw new NetworkException(messageClass + " is already registered as a message!");
        }
    }

    @Override
    public <R extends Record> void addClientboundMessage(Class<R> messageClass, Endec<R> endec, Consumer<R> handler) {
        int id = this.owo$clientboundMessages.size();

        var messageData = new ScreenhandlerMessageData<>(id, true, endec, handler);
        this.owo$clientboundMessages.add(messageData);

        if (this.owo$messages.put(messageClass, messageData) != null) {
            throw new NetworkException(messageClass + " is already registered as a message!");
        }
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <R extends Record> void sendMessage(@NotNull R message) {
        if (this.owo$player == null) {
            throw new NetworkException("Tried to send a message before player was attached");
        }

        ScreenhandlerMessageData messageData = this.owo$messages.get(message.getClass());

        if (messageData == null) {
            throw new NetworkException("Tried to send message of unknown type " + message.getClass());
        }

        var buf = PacketByteBufs.create();
        buf.writeVarInt(messageData.id());
        buf.write(messageData.endec(), message);

        if (messageData.clientbound()) {
            if (!(this.owo$player instanceof ServerPlayer serverPlayer)) {
                throw new NetworkException("Tried to send clientbound message on the server");
            }

            ServerPlayNetworking.send(serverPlayer, ScreenInternals.LOCAL_PACKET, buf);
        } else {
            if (!this.owo$player.level().isClientSide) {
                throw new NetworkException("Tried to send serverbound message on the client");
            }

            this.owo$sendToServer(ScreenInternals.LOCAL_PACKET, buf);
        }
    }

    @Environment(EnvType.CLIENT)
    private void owo$sendToServer(ResourceLocation channel, FriendlyByteBuf data) {
        ClientPlayNetworking.send(channel, data);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void owo$handlePacket(FriendlyByteBuf buf, boolean clientbound) {
        int id = buf.readVarInt();
        ScreenhandlerMessageData messageData = (clientbound ? this.owo$clientboundMessages : this.owo$serverboundMessages).get(id);

        messageData.handler().accept(buf.read(messageData.endec()));
    }

    @Override
    public <T> SyncedProperty<T> createProperty(Class<T> clazz, Endec<T> endec, T initial) {
        var prop = new SyncedProperty<>(this.owo$properties.size(), endec, initial);
        this.owo$properties.add(prop);
        return prop;
    }

    @Override
    public void owo$readPropertySync(FriendlyByteBuf buf) {
        int count = buf.readVarInt();

        for (int i = 0; i < count; i++) {
            int idx = buf.readVarInt();
            this.owo$properties.get(idx).read(buf);
        }
    }

    @Inject(method = "sendAllDataToRemote", at = @At("RETURN"))
    private void syncOnSyncState(CallbackInfo ci) {
        this.syncProperties();
    }

    @Inject(method = "broadcastChanges", at = @At("RETURN"))
    private void syncOnSendContentUpdates(CallbackInfo ci) {
        if (suppressRemoteUpdates) return;

        this.syncProperties();
    }

    private void syncProperties() {
        if (this.owo$player == null) return;
        if (!(this.owo$player instanceof ServerPlayer player)) return;

        int count = 0;

        for (var property : this.owo$properties) {
            if (property.needsSync()) count++;
        }

        if (count == 0) return;

        var buf = PacketByteBufs.create();
        buf.writeVarInt(count);

        for (var prop : owo$properties) {
            if (!prop.needsSync()) continue;

            buf.writeVarInt(prop.index());
            prop.write(buf);
        }

        ServerPlayNetworking.send(player, ScreenInternals.SYNC_PROPERTIES, buf);
    }

}
