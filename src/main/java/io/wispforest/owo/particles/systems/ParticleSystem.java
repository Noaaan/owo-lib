package io.wispforest.owo.particles.systems;

import io.wispforest.owo.network.NetworkException;
import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.util.OwoFreezer;
import io.wispforest.owo.util.ServicesFrozenException;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a particle effect that can be played
 * at a position in a world <i>on both client and server</i>,
 * with some optional data attached.
 * <br>
 * To run this effect, call {@link #spawn(Level, Vec3, Object)}. If you call this
 * on the server, a command will be sent to the client to execute the system.
 * <b>Thus, it is important this is registered on both client and server</b>
 * <p>
 * In case your particle effect not required any additional data,
 * use {@link Void} as the data class and pass {@code null} to {@link #spawn(Level, Vec3, Object)}
 *
 * @param <T> The data class
 */
public class ParticleSystem<T> {

    private final ParticleSystemController manager;

    final Class<T> dataClass;
    final int index;
    final Endec<T> endec;
    ParticleSystemExecutor<T> handler;

    private final boolean permitsContextlessExecution;

    ParticleSystem(ParticleSystemController manager, Class<T> dataClass, int index, Endec<T> endec, ParticleSystemExecutor<T> handler) {
        OwoFreezer.checkRegister("Particle systems");

        this.manager = manager;
        this.dataClass = dataClass;
        this.index = index;
        this.endec = endec;
        this.handler = handler;

        this.permitsContextlessExecution = dataClass == Void.class;
    }

    /**
     * Sets the particle system's handler.
     *
     * @param handler the code that is run to actually display the particle system
     * @throws NetworkException if this particle system already has a handler
     */
    public void setHandler(ParticleSystemExecutor<T> handler) {
        if (OwoFreezer.isFrozen()) throw new ServicesFrozenException("Particle systems can only be changed during mod init");
        if (this.handler != null) throw new NetworkException("Particle system already has a handler");

        this.handler = handler;
    }

    /**
     * Spawns, or displays, whichever term you prefer,
     * this particle system in the given world at the
     * given position and with the passed context data
     *
     * <p><b>{@code null} data is only allowed if the data class of this
     * particle system is {@link Void}</b>
     *
     * @param world The world to execute in
     * @param pos   The position to execute at
     * @param data  The context to execute with
     */
    public void spawn(Level world, Vec3 pos, @Nullable T data) {
        if (data == null && !permitsContextlessExecution) throw new IllegalStateException("This particle system does not permit 'null' data");

        if (world.isClientSide) {
            handler.executeParticleSystem(world, pos, data);
        } else {
            manager.sendPacket(this, (ServerLevel) world, pos, data);
        }
    }

    /**
     * Convenience wrapper for {@link #spawn(Level, Vec3, Object)}
     * that always passes {@code null} data
     *
     * @param world The world to execute in
     * @param pos   The position to execute at
     */
    public void spawn(Level world, Vec3 pos) {
        spawn(world, pos, null);
    }
}
