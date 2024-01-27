package io.wispforest.owo.config;

import com.google.common.collect.HashMultimap;
import io.wispforest.owo.Owo;
import io.wispforest.owo.config.Option.Key;
import io.wispforest.owo.mixin.ServerCommonNetworkHandlerAccessor;
import io.wispforest.owo.ops.TextOps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

public class ConfigSynchronizer {

    public static final ResourceLocation CONFIG_SYNC_CHANNEL = new ResourceLocation("owo", "config_sync");

    private static final Map<Connection, Map<String, Map<Option.Key, Object>>> CLIENT_OPTION_STORAGE = new WeakHashMap<>();

    private static final Map<String, ConfigWrapper<?>> KNOWN_CONFIGS = new HashMap<>();
    private static final MutableComponent PREFIX = TextOps.concat(Owo.PREFIX, Component.nullToEmpty("§cunrecoverable config mismatch\n\n"));

    static void register(ConfigWrapper<?> config) {
        KNOWN_CONFIGS.put(config.name(), config);
    }

    /**
     * Retrieve the options which the given player's client
     * sent to the server during config synchronization
     *
     * @param player     The player for which to retrieve the client values
     * @param configName The name of the config for which to retrieve values
     * @return The player's client's values of the given config options,
     * or {@code null} if no config with the given name was synced
     */
    public static @Nullable Map<Option.Key, ?> getClientOptions(ServerPlayer player, String configName) {
        var storage = CLIENT_OPTION_STORAGE.get(((ServerCommonNetworkHandlerAccessor) player.connection).owo$getConnection());
        if (storage == null) return null;

        return storage.get(configName);
    }

    /**
     * Safer, more clear version of {@link #getClientOptions(ServerPlayer, String)} to
     * be used when the actual config wrapper is available
     *
     * @see #getClientOptions(ServerPlayer, String)
     */
    public static @Nullable Map<Option.Key, ?> getClientOptions(ServerPlayer player, ConfigWrapper<?> config) {
        return getClientOptions(player, config.name());
    }

    private static void write(FriendlyByteBuf packet, Option.SyncMode targetMode) {
        packet.writeVarInt(KNOWN_CONFIGS.size());

        var configBuf = PacketByteBufs.create();
        var optionBuf = PacketByteBufs.create();

        KNOWN_CONFIGS.forEach((configName, config) -> {
            packet.writeUtf(configName);

            configBuf.resetReaderIndex().resetWriterIndex();
            configBuf.writeVarInt((int) config.allOptions().values().stream().filter(option -> option.syncMode().ordinal() >= targetMode.ordinal()).count());

            config.allOptions().forEach((key, option) -> {
                if (option.syncMode().ordinal() < targetMode.ordinal()) return;

                configBuf.writeUtf(key.asString());

                optionBuf.resetReaderIndex().resetWriterIndex();
                option.write(optionBuf);

                configBuf.writeVarInt(optionBuf.readableBytes());
                configBuf.writeBytes(optionBuf);
            });

            packet.writeVarInt(configBuf.readableBytes());
            packet.writeBytes(configBuf);
        });
    }

    private static void read(FriendlyByteBuf buf, BiConsumer<Option<?>, FriendlyByteBuf> optionConsumer) {
        int configCount = buf.readVarInt();
        for (int i = 0; i < configCount; i++) {
            var configName = buf.readUtf();
            var config = KNOWN_CONFIGS.get(configName);
            if (config == null) {
                Owo.LOGGER.error("Received overrides for unknown config '{}', skipping", configName);

                // skip size of current config
                buf.skipBytes(buf.readVarInt());
                continue;
            }

            // ignore size
            buf.readVarInt();

            int optionCount = buf.readVarInt();
            for (int j = 0; j < optionCount; j++) {
                var optionKey = new Option.Key(buf.readUtf());
                var option = config.optionForKey(optionKey);
                if (option == null) {
                    Owo.LOGGER.error("Received override for unknown option '{}' in config '{}', skipping", optionKey, configName);

                    // skip size of current option
                    buf.skipBytes(buf.readVarInt());
                    continue;
                }

                // ignore size
                buf.readVarInt();

                optionConsumer.accept(option, buf);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private static void applyClient(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender sender) {
        Owo.LOGGER.info("Applying server overrides");
        var mismatchedOptions = new HashMap<Option<?>, Object>();

        if (!(client.hasSingleplayerServer() && client.getSingleplayerServer().isSingleplayer())) {
            read(buf, (option, packetByteBuf) -> {
                var mismatchedValue = option.read(packetByteBuf);
                if (mismatchedValue != null) mismatchedOptions.put(option, mismatchedValue);
            });

            if (!mismatchedOptions.isEmpty()) {
                Owo.LOGGER.error("Aborting connection, non-syncable config values were mismatched");
                mismatchedOptions.forEach((option, serverValue) -> {
                    Owo.LOGGER.error("- Option {} in config '{}' has value '{}' but server requires '{}'",
                            option.key().asString(), option.configName(), option.value(), serverValue);
                });

                var errorMessage = Component.empty();
                var optionsByConfig = HashMultimap.<String, Tuple<Option<?>, Object>>create();

                mismatchedOptions.forEach((option, serverValue) -> optionsByConfig.put(option.configName(), new Tuple<>(option, serverValue)));
                for (var configName : optionsByConfig.keys()) {
                    errorMessage.append(TextOps.withFormatting("in config ", ChatFormatting.GRAY)).append(configName).append("\n");
                    for (var option : optionsByConfig.get(configName)) {
                        errorMessage.append(Component.translatable(option.getA().translationKey()).withStyle(ChatFormatting.YELLOW)).append(" -> ");
                        errorMessage.append(option.getA().value().toString()).append(TextOps.withFormatting(" (client)", ChatFormatting.GRAY));
                        errorMessage.append(TextOps.withFormatting(" / ", ChatFormatting.DARK_GRAY));
                        errorMessage.append(option.getB().toString()).append(TextOps.withFormatting(" (server)", ChatFormatting.GRAY)).append("\n");
                    }
                    errorMessage.append("\n");
                }

                errorMessage.append(TextOps.withFormatting("these options could not be synchronized because\n", ChatFormatting.GRAY));
                errorMessage.append(TextOps.withFormatting("they require your client to be restarted\n", ChatFormatting.GRAY));
                errorMessage.append(TextOps.withFormatting("change them manually and restart if you want to join this server", ChatFormatting.GRAY));

                handler.getConnection().disconnect(TextOps.concat(PREFIX, errorMessage));
                return;
            }
        }

        Owo.LOGGER.info("Responding with client values");
        var packet = PacketByteBufs.create();
        write(packet, Option.SyncMode.INFORM_SERVER);

        sender.sendPacket(CONFIG_SYNC_CHANNEL, packet);
    }

    private static void applyServer(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender sender) {
        Owo.LOGGER.info("Receiving client config");
        var connection = ((ServerCommonNetworkHandlerAccessor) player.connection).owo$getConnection();

        read(buf, (option, optionBuf) -> {
            var config = CLIENT_OPTION_STORAGE.computeIfAbsent(connection, $ -> new HashMap<>()).computeIfAbsent(option.configName(), s -> new HashMap<>());
            config.put(option.key(), optionBuf.read(option.endec()));
        });
    }

    static {
        var earlyPhase = new ResourceLocation("owo", "early");
        ServerPlayConnectionEvents.JOIN.addPhaseOrdering(earlyPhase, Event.DEFAULT_PHASE);
        ServerPlayConnectionEvents.JOIN.register(earlyPhase, (handler, sender, server) -> {
            Owo.LOGGER.info("Sending server config values to client");

            var packet = PacketByteBufs.create();
            write(packet, Option.SyncMode.OVERRIDE_CLIENT);

            sender.sendPacket(CONFIG_SYNC_CHANNEL, packet);
        });

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientPlayNetworking.registerGlobalReceiver(CONFIG_SYNC_CHANNEL, ConfigSynchronizer::applyClient);

            ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
                KNOWN_CONFIGS.forEach((name, config) -> config.forEachOption(Option::reattach));
            });
        }

        ServerPlayNetworking.registerGlobalReceiver(CONFIG_SYNC_CHANNEL, ConfigSynchronizer::applyServer);
    }
}
