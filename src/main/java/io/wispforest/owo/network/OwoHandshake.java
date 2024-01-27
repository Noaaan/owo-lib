package io.wispforest.owo.network;

import io.wispforest.owo.Owo;
import io.wispforest.owo.mixin.ClientCommonNetworkHandlerAccessor;
import io.wispforest.owo.mixin.ServerCommonNetworkHandlerAccessor;
import io.wispforest.owo.ops.TextOps;
import io.wispforest.owo.particles.systems.ParticleSystemController;
import io.wispforest.owo.serialization.endec.BuiltInEndecs;
import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.util.OwoFreezer;
import io.wispforest.owo.util.ServicesFrozenException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientConfigurationPacketListenerImpl;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.ToIntFunction;

@ApiStatus.Internal
public final class OwoHandshake {

    private static final Endec<Map<ResourceLocation, Integer>> CHANNEL_HASHES_ENDEC = Endec.map(BuiltInEndecs.IDENTIFIER, Endec.INT);

    private static final MutableComponent PREFIX = TextOps.concat(Owo.PREFIX, Component.nullToEmpty("§chandshake failure\n"));
    public static final ResourceLocation CHANNEL_ID = new ResourceLocation("owo", "handshake");
    public static final ResourceLocation OFF_CHANNEL_ID = new ResourceLocation("owo", "handshake_off");

    private static final boolean ENABLED = System.getProperty("owo.handshake.enabled") != null ? Boolean.getBoolean("owo.handshake.enabled") : Owo.DEBUG;
    private static boolean HANDSHAKE_REQUIRED = false;
    private static boolean QUERY_RECEIVED = false;

    private OwoHandshake() {}

    // ------------
    // Registration
    // ------------

    public static void enable() {
        if (OwoFreezer.isFrozen()) {
            throw new ServicesFrozenException("The oωo handshake may only be enabled during mod initialization");
        }
    }

    public static void requireHandshake() {
        if (OwoFreezer.isFrozen()) {
            throw new ServicesFrozenException("The oωo handshake may only be made required during mod initialization");
        }

        HANDSHAKE_REQUIRED = true;
    }

    static {
        ServerConfigurationConnectionEvents.CONFIGURE.register(OwoHandshake::configureStart);
        ServerConfigurationNetworking.registerGlobalReceiver(OwoHandshake.CHANNEL_ID, OwoHandshake::syncServer);

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            if (!ENABLED) {
                ClientConfigurationNetworking.registerGlobalReceiver(OwoHandshake.OFF_CHANNEL_ID, (client, handler, buf, responseSender) -> {});
            }

            ClientConfigurationNetworking.registerGlobalReceiver(OwoHandshake.CHANNEL_ID, OwoHandshake::syncClient);
            ClientConfigurationConnectionEvents.READY.register(OwoHandshake::handleReadyClient);

            ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> QUERY_RECEIVED = false);
            ClientConfigurationConnectionEvents.DISCONNECT.register((handler, client) -> QUERY_RECEIVED = false);
        }
    }

    public static boolean isValidClient() {
        return ENABLED && QUERY_RECEIVED;
    }

    // -------
    // Packets
    // -------

    private static void configureStart(ServerConfigurationPacketListenerImpl handler, MinecraftServer server) {
        if (!ENABLED) return;

        if (ServerConfigurationNetworking.canSend(handler, OFF_CHANNEL_ID)) {
            Owo.LOGGER.info("[Handshake] Handshake disabled by client, skipping");
            return;
        }

        if (!ServerConfigurationNetworking.canSend(handler, CHANNEL_ID)) {
            if (!HANDSHAKE_REQUIRED) return;

            handler.disconnect(TextOps.concat(PREFIX, Component.nullToEmpty("incompatible client")));
            Owo.LOGGER.info("[Handshake] Handshake failed, client doesn't understand channel packet");
            return;
        }

        var request = PacketByteBufs.create();
        writeHashes(request, OwoNetChannel.OPTIONAL_CHANNELS, OwoHandshake::hashChannel);
        ServerConfigurationNetworking.send(handler, OwoHandshake.CHANNEL_ID, request);
        Owo.LOGGER.info("[Handshake] Sending channel packet");
    }

    @Environment(EnvType.CLIENT)
    private static void syncClient(Minecraft client, ClientConfigurationPacketListenerImpl handler, FriendlyByteBuf buf, PacketSender sender) {
        Owo.LOGGER.info("[Handshake] Sending client channels");
        QUERY_RECEIVED = true;

        if (buf.readableBytes() > 0) {
            final var serverOptionalChannels = buf.read(CHANNEL_HASHES_ENDEC);
            ((OwoClientConnectionExtension) ((ClientCommonNetworkHandlerAccessor) handler).getConnection()).owo$setChannelSet(filterOptionalServices(serverOptionalChannels, OwoNetChannel.REGISTERED_CHANNELS, OwoHandshake::hashChannel));
        }

        var response = PacketByteBufs.create();
        writeHashes(response, OwoNetChannel.REQUIRED_CHANNELS, OwoHandshake::hashChannel);
        writeHashes(response, ParticleSystemController.REGISTERED_CONTROLLERS, OwoHandshake::hashController);
        writeHashes(response, OwoNetChannel.OPTIONAL_CHANNELS, OwoHandshake::hashChannel);

        sender.sendPacket(CHANNEL_ID, response);
    }

    private static void syncServer(MinecraftServer server, ServerConfigurationPacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
        Owo.LOGGER.info("[Handshake] Receiving client channels");

        final var clientChannels = buf.read(CHANNEL_HASHES_ENDEC);
        final var clientParticleControllers = buf.read(CHANNEL_HASHES_ENDEC);

        StringBuilder disconnectMessage = new StringBuilder();

        boolean isAllGood = verifyReceivedHashes("channels", clientChannels, OwoNetChannel.REQUIRED_CHANNELS, OwoHandshake::hashChannel, disconnectMessage);
        isAllGood &= verifyReceivedHashes("controllers", clientParticleControllers, ParticleSystemController.REGISTERED_CONTROLLERS, OwoHandshake::hashController, disconnectMessage);

        if (!isAllGood) {
            handler.disconnect(TextOps.concat(PREFIX, Component.nullToEmpty(disconnectMessage.toString())));
        }

        if (buf.readableBytes() > 0) {
            final var clientOptionalChannels = buf.read(CHANNEL_HASHES_ENDEC);
            ((OwoClientConnectionExtension) ((ServerCommonNetworkHandlerAccessor) handler).owo$getConnection()).owo$setChannelSet(filterOptionalServices(clientOptionalChannels, OwoNetChannel.OPTIONAL_CHANNELS, OwoHandshake::hashChannel));
        }

        Owo.LOGGER.info("[Handshake] Handshake completed successfully");
    }

    @Environment(EnvType.CLIENT)
    private static void handleReadyClient(ClientConfigurationPacketListenerImpl handler, Minecraft client) {
        if (ClientConfigurationNetworking.canSend(CHANNEL_ID) || !HANDSHAKE_REQUIRED || !ENABLED) return;

        client.execute(() -> {
            ((ClientCommonNetworkHandlerAccessor) handler)
                    .getConnection()
                    .disconnect(TextOps.concat(PREFIX, Component.nullToEmpty("incompatible server")));
        });
    }

    // -------
    // Utility
    // -------

    private static <T> Set<ResourceLocation> filterOptionalServices(Map<ResourceLocation, Integer> remoteMap, Map<ResourceLocation, T> localMap, ToIntFunction<T> hashFunction) {
        Set<ResourceLocation> readableServices = new HashSet<>();

        for (var entry : remoteMap.entrySet()) {
            var service = localMap.get(entry.getKey());

            if (service == null) continue;
            if (hashFunction.applyAsInt(service) != entry.getValue()) continue;

            readableServices.add(entry.getKey());
        }

        return readableServices;
    }

    private static <T> boolean verifyReceivedHashes(String serviceNamePlural, Map<ResourceLocation, Integer> clientMap, Map<ResourceLocation, T> serverMap, ToIntFunction<T> hashFunction, StringBuilder disconnectMessage) {
        boolean isAllGood = true;

        if (!clientMap.keySet().equals(serverMap.keySet())) {
            isAllGood = false;

            var leftovers = findCollisions(clientMap.keySet(), serverMap.keySet());

            if (!leftovers.getA().isEmpty()) {
                disconnectMessage.append("server is missing ").append(serviceNamePlural).append(":\n");
                leftovers.getA().forEach(identifier -> disconnectMessage.append("§7").append(identifier).append("§r\n"));
            }

            if (!leftovers.getB().isEmpty()) {
                disconnectMessage.append("client is missing ").append(serviceNamePlural).append(":\n");
                leftovers.getB().forEach(identifier -> disconnectMessage.append("§7").append(identifier).append("§r\n"));
            }
        }

        boolean hasMismatchedHashes = false;
        for (var entry : clientMap.entrySet()) {
            var actualServiceObject = serverMap.get(entry.getKey());
            if (actualServiceObject == null) continue;

            int localHash = hashFunction.applyAsInt(actualServiceObject);

            if (localHash != entry.getValue()) {
                if (!hasMismatchedHashes) {
                    disconnectMessage.append(serviceNamePlural).append(" with mismatched hashes:\n");
                }

                disconnectMessage.append("§7").append(entry.getKey()).append("§r\n");

                isAllGood = false;
                hasMismatchedHashes = true;
            }
        }

        return isAllGood;
    }

    private static <T> void writeHashes(FriendlyByteBuf buffer, Map<ResourceLocation, T> values, ToIntFunction<T> hashFunction) {
        Map<ResourceLocation, Integer> hashes = new HashMap<>();

        for (var entry : values.entrySet()) {
            hashes.put(entry.getKey(), hashFunction.applyAsInt(entry.getValue()));
        }

        buffer.write(CHANNEL_HASHES_ENDEC, hashes);
    }

    private static Tuple<Set<ResourceLocation>, Set<ResourceLocation>> findCollisions(Set<ResourceLocation> first, Set<ResourceLocation> second) {
        var firstLeftovers = new HashSet<ResourceLocation>();
        var secondLeftovers = new HashSet<ResourceLocation>();

        first.forEach(identifier -> {
            if (!second.contains(identifier)) firstLeftovers.add(identifier);
        });

        second.forEach(identifier -> {
            if (!first.contains(identifier)) secondLeftovers.add(identifier);
        });

        return new Tuple<>(firstLeftovers, secondLeftovers);
    }

    private static int hashChannel(OwoNetChannel channel) {
        int serializersHash = 0;
        for (var entry : channel.endecsByIndex.int2ObjectEntrySet()) {
            serializersHash += entry.getIntKey() * 31 + entry.getValue().getRecordClass().getName().hashCode();
        }
        return 31 * channel.packetId.hashCode() + serializersHash;
    }

    private static int hashController(ParticleSystemController controller) {
        int serializersHash = 0;
        for (var entry : controller.systemsByIndex.int2ObjectEntrySet()) {
            serializersHash += entry.getIntKey();
        }
        return 31 * controller.channelId.hashCode() + serializersHash;
    }
}
