package io.wispforest.owo.particles.systems;

import io.wispforest.owo.Owo;
import io.wispforest.owo.network.NetworkException;
import io.wispforest.owo.network.OwoHandshake;
import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.serialization.endec.ReflectiveEndecBuilder;
import io.wispforest.owo.util.OwoFreezer;
import io.wispforest.owo.util.ReflectionUtils;
import io.wispforest.owo.util.VectorSerializer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * A controller object that manages and creates {@link ParticleSystem}s.
 * It is recommended to have one of these per mod.
 * <p>
 * To obtain a new particle system, call {@link #register(Class, Endec, ParticleSystemExecutor)}
 * with the system's context data class and handler function. <b>It is important
 * that this is done on both client and server, otherwise joining the server
 * will fail in a handshake error</b>
 */
public class ParticleSystemController {

    @ApiStatus.Internal
    public static final Map<ResourceLocation, ParticleSystemController> REGISTERED_CONTROLLERS = new HashMap<>();

    @ApiStatus.Internal
    public final Int2ObjectMap<ParticleSystem<?>> systemsByIndex = new Int2ObjectOpenHashMap<>();

    public final ResourceLocation channelId;
    private int maxIndex = 0;
    private final String ownerClassName;

    /**
     * Creates a new controller with the given ID. Duplicate controller IDs
     * are not allowed - if there is a collision, the name of the
     * class that previously registered the controller will be part of
     * the exception. <b>This may be called at any stage during
     * mod initialization</b>
     *
     * @param channelId The packet ID to use
     */
    public ParticleSystemController(ResourceLocation channelId) {
        OwoFreezer.checkRegister("Particle system controllers");

        if (REGISTERED_CONTROLLERS.containsKey(channelId)) {
            throw new IllegalStateException("Controller with id '" + channelId + "' was already registered from class '" +
                    REGISTERED_CONTROLLERS.get(channelId).ownerClassName + "'");
        }

        this.channelId = channelId;
        this.ownerClassName = ReflectionUtils.getCallingClassName(2);

        OwoHandshake.enable();
        OwoHandshake.requireHandshake();

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientPlayNetworking.registerGlobalReceiver(channelId, new Client()::handler);
        }

        REGISTERED_CONTROLLERS.put(channelId, this);
    }

    /**
     * Registers the given system executor with the given
     * context data class, thereby creating a new system
     *
     * @param dataClass The class to use as context data
     * @param executor  The code that is run to actually display the particle system
     * @param <T>       The type of context data to use
     * @return The created particle system
     */
    public <T> ParticleSystem<T> register(Class<T> dataClass, Endec<T> endec, ParticleSystemExecutor<T> executor) {
        int index = maxIndex++;
        var system = new ParticleSystem<>(this, dataClass, index, endec, executor);
        systemsByIndex.put(index, system);
        return system;
    }

    /**
     * Shorthand for {{@link #register(Class, Endec, ParticleSystemExecutor)}} which creates the endec
     * through {@link ReflectiveEndecBuilder#get(Class)}
     */
    public <T> ParticleSystem<T> register(Class<T> dataClass, ParticleSystemExecutor<T> executor) {
        return this.register(dataClass, ReflectiveEndecBuilder.get(dataClass), executor);
    }

    /**
     * Registers the given system executor with the given
     * context data class, thereby creating a new system
     * <p>
     * This method defers executor registration, so
     * you must register the handler later in a client entrypoint.
     *
     * @param dataClass The class to use as context data
     * @param <T>       The type of context data to use
     * @return The created particle system
     * @see ParticleSystem#setHandler(ParticleSystemExecutor)
     */
    public <T> ParticleSystem<T> registerDeferred(Class<T> dataClass, Endec<T> endec) {
        int index = maxIndex++;
        var system = new ParticleSystem<>(this, dataClass, index, endec, null);
        systemsByIndex.put(index, system);
        return system;
    }

    /**
     * Shorthand for {{@link #registerDeferred(Class, Endec)}} which creates the endec
     * through {@link ReflectiveEndecBuilder#get(Class)}
     */
    public <T> ParticleSystem<T> registerDeferred(Class<T> dataClass) {
        return this.registerDeferred(dataClass, ReflectiveEndecBuilder.get(dataClass));
    }

    <T> void sendPacket(ParticleSystem<T> particleSystem, ServerLevel world, Vec3 pos, T data) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(particleSystem.index);
        VectorSerializer.write(buf, pos);
        buf.write(particleSystem.endec, data);

        for (var player : PlayerLookup.tracking(world, BlockPos.containing(pos))) {
            ServerPlayNetworking.send(player, channelId, buf);
        }
    }

    private void verify() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            for (ParticleSystem<?> system : systemsByIndex.values()) {
                if (system.handler == null) {
                    throw new NetworkException("Some particle systems of " + channelId + " don't have handlers registered");
                }
            }
        }
    }

    static {
        OwoFreezer.registerFreezeCallback(() -> {
            for (ParticleSystemController controller : REGISTERED_CONTROLLERS.values()) {
                controller.verify();
            }
        });
    }

    @Environment(EnvType.CLIENT)
    private class Client {
        @SuppressWarnings("unchecked")
        private void handler(Minecraft client, ClientPacketListener networkHandler, FriendlyByteBuf buf, PacketSender sender) {
            int index = buf.readVarInt();

            Vec3 pos = VectorSerializer.read(buf);

            if (maxIndex <= index || index < 0) {
                Owo.LOGGER.warn("Received unknown particle system index {} on channel {}", index, channelId);
                return;
            }

            ParticleSystem<Object> system = (ParticleSystem<Object>) systemsByIndex.get(index);
            var data = buf.read(system.endec);
            client.execute(() -> system.handler.executeParticleSystem(client.level, pos, data));
        }
    }
}
