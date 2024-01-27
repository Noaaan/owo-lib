package io.wispforest.owo.serialization.endec;


import com.mojang.datafixers.util.Function3;
import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.serialization.SerializationAttribute;
import io.wispforest.owo.serialization.format.nbt.NbtEndec;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.time.Instant;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

public final class BuiltInEndecs {

    private BuiltInEndecs() {}

    // --- Java Types ---

    public static final Endec<int[]> INT_ARRAY = Endec.INT.listOf().xmap((list) -> list.stream().mapToInt(v -> v).toArray(), (ints) -> Arrays.stream(ints).boxed().toList());
    public static final Endec<long[]> LONG_ARRAY = Endec.LONG.listOf().xmap((list) -> list.stream().mapToLong(v -> v).toArray(), (longs) -> Arrays.stream(longs).boxed().toList());

    public static final Endec<BitSet> BITSET = LONG_ARRAY.xmap(BitSet::valueOf, BitSet::toLongArray);

    public static final Endec<java.util.UUID> UUID = Endec
            .ifAttr(
                    SerializationAttribute.HUMAN_READABLE,
                    Endec.STRING.xmap(java.util.UUID::fromString, java.util.UUID::toString)
            ).orElse(
                    INT_ARRAY.xmap(UUIDUtil::uuidFromIntArray, UUIDUtil::uuidToIntArray)
            );

    public static final Endec<Date> DATE = Endec
            .ifAttr(
                    SerializationAttribute.HUMAN_READABLE,
                    Endec.STRING.xmap(s -> Date.from(Instant.parse(s)), date -> date.toInstant().toString())
            ).orElse(
                    Endec.LONG.xmap(Date::new, Date::getTime)
            );

    // --- MC Types ---

    public static final Endec<ResourceLocation> IDENTIFIER = Endec.STRING.xmap(ResourceLocation::new, ResourceLocation::toString);
    public static final Endec<ItemStack> ITEM_STACK = NbtEndec.COMPOUND.xmap(ItemStack::of, stack -> stack.save(new CompoundTag()));
    public static final Endec<Component> TEXT = Endec.ofCodec(ComponentSerialization.CODEC);

    public static final Endec<Vec3i> VEC3I = vectorEndec("Vec3i", Endec.INT, Vec3i::new, Vec3i::getX, Vec3i::getY, Vec3i::getZ);
    public static final Endec<Vec3> VEC3D = vectorEndec("Vec3d", Endec.DOUBLE, Vec3::new, Vec3::x, Vec3::y, Vec3::z);
    public static final Endec<Vector3f> VECTOR3F = vectorEndec("Vector3f", Endec.FLOAT, Vector3f::new, Vector3f::x, Vector3f::y, Vector3f::z);

    public static final Endec<BlockPos> BLOCK_POS = Endec
            .ifAttr(
                    SerializationAttribute.HUMAN_READABLE,
                    vectorEndec("BlockPos", Endec.INT, BlockPos::new, BlockPos::getX, BlockPos::getY, BlockPos::getZ)
            ).orElse(
                    Endec.LONG.xmap(BlockPos::of, BlockPos::asLong)
            );

    public static final Endec<ChunkPos> CHUNK_POS = Endec
            .ifAttr(
                    SerializationAttribute.HUMAN_READABLE,
                    Endec.INT.listOf().validate(ints -> {
                        if (ints.size() != 2) {
                            throw new IllegalStateException("ChunkPos array must have two elements");
                        }
                    }).xmap(
                            ints -> new ChunkPos(ints.get(0), ints.get(1)),
                            chunkPos -> List.of(chunkPos.x, chunkPos.z)
                    )
            )
            .orElse(Endec.LONG.xmap(ChunkPos::new, ChunkPos::toLong));

    public static final Endec<FriendlyByteBuf> PACKET_BYTE_BUF = Endec.BYTES
            .xmap(bytes -> {
                var buffer = PacketByteBufs.create();
                buffer.writeBytes(bytes);

                return buffer;
            }, buffer -> {
                var bytes = new byte[buffer.readableBytes()];
                buffer.readBytes(bytes);

                return bytes;
            });

    // --- Constructors for MC types ---

    public static <T> Endec<T> ofRegistry(Registry<T> registry) {
        return IDENTIFIER.xmap(registry::get, registry::getKey);
    }

    public static <T> Endec<TagKey<T>> unprefixedTagKey(ResourceKey<? extends Registry<T>> registry) {
        return IDENTIFIER.xmap(id -> TagKey.create(registry, id), TagKey::location);
    }

    public static <T> Endec<TagKey<T>> prefixedTagKey(ResourceKey<? extends Registry<T>> registry) {
        return Endec.STRING.xmap(
                s -> TagKey.create(registry, new ResourceLocation(s.substring(1))),
                tag -> "#" + tag.location()
        );
    }

    private static <C, V> Endec<V> vectorEndec(String name, Endec<C> componentEndec, Function3<C, C, C, V> constructor, Function<V, C> xGetter, Function<V, C> yGetter, Function<V, C> zGetter) {
        return componentEndec.listOf().validate(ints -> {
            if (ints.size() != 3) {
                throw new IllegalStateException(name + " array must have three elements");
            }
        }).xmap(
                components -> constructor.apply(components.get(0), components.get(1), components.get(2)),
                vector -> List.of(xGetter.apply(vector), yGetter.apply(vector), zGetter.apply(vector))
        );
    }
}
