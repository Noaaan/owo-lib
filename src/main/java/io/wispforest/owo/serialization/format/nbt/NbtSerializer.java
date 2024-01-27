package io.wispforest.owo.serialization.format.nbt;

import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.serialization.SerializationAttribute;
import io.wispforest.owo.serialization.Serializer;
import io.wispforest.owo.serialization.util.RecursiveSerializer;
import net.minecraft.nbt.*;
import net.minecraft.network.VarInt;
import net.minecraft.network.VarLong;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public class NbtSerializer extends RecursiveSerializer<Tag> {

    private static final Set<SerializationAttribute> ATTRIBUTES = EnumSet.of(
            SerializationAttribute.SELF_DESCRIBING
    );

    protected Tag prefix;

    protected NbtSerializer(Tag prefix) {
        super(EndTag.INSTANCE);
        this.prefix = prefix;
    }

    public static NbtSerializer of(Tag prefix) {
        return new NbtSerializer(prefix);
    }

    public static NbtSerializer of() {
        return of(null);
    }

    // ---

    @Override
    public Set<SerializationAttribute> attributes() {
        return ATTRIBUTES;
    }

    // ---

    @Override
    public void writeByte(byte value) {
        this.consume(ByteTag.valueOf(value));
    }

    @Override
    public void writeShort(short value) {
        this.consume(ShortTag.valueOf(value));
    }

    @Override
    public void writeInt(int value) {
        this.consume(IntTag.valueOf(value));
    }

    @Override
    public void writeLong(long value) {
        this.consume(LongTag.valueOf(value));
    }

    @Override
    public void writeFloat(float value) {
        this.consume(FloatTag.valueOf(value));
    }

    @Override
    public void writeDouble(double value) {
        this.consume(DoubleTag.valueOf(value));
    }

    // ---

    @Override
    public void writeVarInt(int value) {
        this.consume(switch (VarInt.getByteSize(value)) {
            case 0, 1 -> ByteTag.valueOf((byte) value);
            case 2 -> ShortTag.valueOf((short) value);
            default -> IntTag.valueOf(value);
        });
    }

    @Override
    public void writeVarLong(long value) {
        this.consume(switch (VarLong.getByteSize(value)) {
            case 0, 1 -> ByteTag.valueOf((byte) value);
            case 2 -> ShortTag.valueOf((short) value);
            case 3, 4 -> IntTag.valueOf((int) value);
            default -> LongTag.valueOf(value);
        });
    }

    // ---

    @Override
    public void writeBoolean(boolean value) {
        this.consume(ByteTag.valueOf(value));
    }

    @Override
    public void writeString(String value) {
        this.consume(StringTag.valueOf(value));
    }

    @Override
    public void writeBytes(byte[] bytes) {
        this.consume(new ByteArrayTag(bytes));
    }

    @Override
    public <V> void writeOptional(Endec<V> endec, Optional<V> optional) {
        if (this.isWritingStructField()) {
            optional.ifPresent(v -> endec.encode(this, v));
        } else {
            try (var struct = this.struct()) {
                struct.field("present", Endec.BOOLEAN, optional.isPresent());
                optional.ifPresent(value -> struct.field("value", endec, value));
            }
        }
    }

    // ---

    @Override
    public <E> Serializer.Sequence<E> sequence(Endec<E> elementEndec, int size) {
        return new io.wispforest.owo.serialization.format.nbt.NbtSerializer.Sequence<>(elementEndec);
    }

    @Override
    public <V> Serializer.Map<V> map(Endec<V> valueEndec, int size) {
        return new io.wispforest.owo.serialization.format.nbt.NbtSerializer.Map<>(valueEndec);
    }

    @Override
    public Struct struct() {
        return new io.wispforest.owo.serialization.format.nbt.NbtSerializer.Map<>(null);
    }

    // ---

    private class Map<V> implements Serializer.Map<V>, Struct {

        private final Endec<V> valueEndec;
        private final CompoundTag result;

        private Map(Endec<V> valueEndec) {
            this.valueEndec = valueEndec;

            if (NbtSerializer.this.prefix != null) {
                if (NbtSerializer.this.prefix instanceof CompoundTag prefixMap) {
                    this.result = prefixMap;
                } else {
                    throw new IllegalStateException("Incompatible prefix of type " + NbtSerializer.this.prefix.getClass().getSimpleName() + " provided for NBT map/struct");
                }
            } else {
                this.result = new CompoundTag();
            }
        }

        @Override
        public void entry(String key, V value) {
            NbtSerializer.this.frame(encoded -> {
                this.valueEndec.encode(NbtSerializer.this, value);
                this.result.put(key, encoded.require("map value"));
            }, false);
        }

        @Override
        public <F> Struct field(String name, Endec<F> endec, F value) {
            NbtSerializer.this.frame(encoded -> {
                endec.encode(NbtSerializer.this, value);
                if (encoded.wasEncoded()) {
                    this.result.put(name, encoded.get());
                }
            }, true);

            return this;
        }

        @Override
        public void end() {
            NbtSerializer.this.consume(this.result);
        }
    }

    private class Sequence<V> implements Serializer.Sequence<V> {

        private final Endec<V> valueEndec;
        private final ListTag result;

        private Sequence(Endec<V> valueEndec) {
            this.valueEndec = valueEndec;

            if (NbtSerializer.this.prefix != null) {
                if (NbtSerializer.this.prefix instanceof ListTag prefixList) {
                    this.result = prefixList;
                } else {
                    throw new IllegalStateException("Incompatible prefix of type " + NbtSerializer.this.prefix.getClass().getSimpleName() + " provided for NBT sequence");
                }
            } else {
                this.result = new ListTag();
            }
        }

        @Override
        public void element(V element) {
            NbtSerializer.this.frame(encoded -> {
                this.valueEndec.encode(NbtSerializer.this, element);
                this.result.add(encoded.require("sequence element"));
            }, false);
        }

        @Override
        public void end() {
            NbtSerializer.this.consume(this.result);
        }
    }
}
