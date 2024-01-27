package io.wispforest.owo.serialization.format.nbt;

import io.wispforest.owo.serialization.*;
import io.wispforest.owo.serialization.util.RecursiveDeserializer;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class NbtDeserializer extends RecursiveDeserializer<Tag> implements SelfDescribedDeserializer<Tag> {

    private static final Set<SerializationAttribute> ATTRIBUTES = EnumSet.of(
            SerializationAttribute.SELF_DESCRIBING
    );

    protected NbtDeserializer(Tag element) {
        super(element);
    }

    public static NbtDeserializer of(Tag element) {
        return new NbtDeserializer(element);
    }

    private <N extends Tag> N getAs(Tag element, Class<N> clazz) {
        if (clazz.isInstance(element)) {
            return clazz.cast(element);
        } else {
            throw new IllegalStateException("Expected a " + clazz.getSimpleName() + ", found a " + element.getClass().getSimpleName());
        }
    }

    // ---

    @Override
    public Set<SerializationAttribute> attributes() {
        return ATTRIBUTES;
    }

    // ---

    @Override
    public byte readByte() {
        return this.getAs(this.getValue(), ByteTag.class).getAsByte();
    }

    @Override
    public short readShort() {
        return this.getAs(this.getValue(), ShortTag.class).getAsShort();
    }

    @Override
    public int readInt() {
        return this.getAs(this.getValue(), IntTag.class).getAsInt();
    }

    @Override
    public long readLong() {
        return this.getAs(this.getValue(), LongTag.class).getAsLong();
    }

    @Override
    public float readFloat() {
        return this.getAs(this.getValue(), FloatTag.class).getAsFloat();
    }

    @Override
    public double readDouble() {
        return this.getAs(this.getValue(), DoubleTag.class).getAsDouble();
    }

    // ---

    @Override
    public int readVarInt() {
        return this.getAs(this.getValue(), NumericTag.class).getAsInt();
    }

    @Override
    public long readVarLong() {
        return this.getAs(this.getValue(), NumericTag.class).getAsLong();
    }

    // ---

    @Override
    public boolean readBoolean() {
        return this.getAs(this.getValue(), ByteTag.class).getAsByte() != 0;
    }

    @Override
    public String readString() {
        return this.getAs(this.getValue(), StringTag.class).getAsString();
    }

    @Override
    public byte[] readBytes() {
        return this.getAs(this.getValue(), ByteArrayTag.class).getAsByteArray();
    }

    @Override
    public <V> Optional<V> readOptional(Endec<V> endec) {
        if (this.isReadingStructField()) {
            return Optional.of(endec.decode(this));
        } else {
            var struct = this.struct();
            return struct.field("present", Endec.BOOLEAN)
                    ? Optional.of(struct.field("value", endec))
                    : Optional.empty();
        }
    }

    // ---

    @Override
    public <E> Deserializer.Sequence<E> sequence(Endec<E> elementEndec) {
        //noinspection unchecked
        return new io.wispforest.owo.serialization.format.nbt.NbtDeserializer.Sequence<>(elementEndec, this.getAs(this.getValue(), CollectionTag.class));
    }

    @Override
    public <V> Deserializer.Map<V> map(Endec<V> valueEndec) {
        return new io.wispforest.owo.serialization.format.nbt.NbtDeserializer.Map<>(valueEndec, this.getAs(this.getValue(), CompoundTag.class));
    }

    @Override
    public Deserializer.Struct struct() {
        return new io.wispforest.owo.serialization.format.nbt.NbtDeserializer.Struct(this.getAs(this.getValue(), CompoundTag.class));
    }

    // ---

    @Override
    public <S> void readAny(Serializer<S> visitor) {
        this.decodeValue(visitor, this.getValue());
    }

    private <S> void decodeValue(Serializer<S> visitor, Tag value) {
        switch (value.getId()) {
            case Tag.TAG_BYTE -> visitor.writeByte(((ByteTag) value).getAsByte());
            case Tag.TAG_SHORT -> visitor.writeShort(((ShortTag) value).getAsShort());
            case Tag.TAG_INT -> visitor.writeInt(((IntTag) value).getAsInt());
            case Tag.TAG_LONG -> visitor.writeLong(((LongTag) value).getAsLong());
            case Tag.TAG_FLOAT -> visitor.writeFloat(((FloatTag) value).getAsFloat());
            case Tag.TAG_DOUBLE -> visitor.writeDouble(((DoubleTag) value).getAsDouble());
            case Tag.TAG_STRING -> visitor.writeString(value.getAsString());
            case Tag.TAG_BYTE_ARRAY -> visitor.writeBytes(((ByteArrayTag) value).getAsByteArray());
            case Tag.TAG_INT_ARRAY, Tag.TAG_LONG_ARRAY, Tag.TAG_LIST -> {
                var list = (CollectionTag<?>) value;
                try (var sequence = visitor.sequence(Endec.<Tag>of(this::decodeValue, deserializer -> null), list.size())) {
                    list.forEach(sequence::element);
                }
            }
            case Tag.TAG_COMPOUND -> {
                var compound = (CompoundTag) value;
                try (var map = visitor.map(Endec.<Tag>of(this::decodeValue, deserializer -> null), compound.size())) {
                    for (var key : compound.getAllKeys()) {
                        map.entry(key, compound.get(key));
                    }
                }
            }
            default ->
                    throw new IllegalArgumentException("Non-standard, unrecognized NbtElement implementation cannot be decoded");
        }
    }

    // ---

    private class Sequence<V> implements Deserializer.Sequence<V> {

        private final Endec<V> valueEndec;
        private final Iterator<Tag> elements;
        private final int size;

        private Sequence(Endec<V> valueEndec, List<Tag> elements) {
            this.valueEndec = valueEndec;

            this.elements = elements.iterator();
            this.size = elements.size();
        }

        @Override
        public int estimatedSize() {
            return this.size;
        }

        @Override
        public boolean hasNext() {
            return this.elements.hasNext();
        }

        @Override
        public V next() {
            return NbtDeserializer.this.frame(
                    this.elements::next,
                    () -> this.valueEndec.decode(NbtDeserializer.this),
                    false
            );
        }
    }

    private class Map<V> implements Deserializer.Map<V> {

        private final Endec<V> valueEndec;
        private final CompoundTag compound;
        private final Iterator<String> keys;
        private final int size;

        private Map(Endec<V> valueEndec, CompoundTag compound) {
            this.valueEndec = valueEndec;

            this.compound = compound;
            this.keys = compound.getAllKeys().iterator();
            this.size = compound.size();
        }

        @Override
        public int estimatedSize() {
            return this.size;
        }

        @Override
        public boolean hasNext() {
            return this.keys.hasNext();
        }

        @Override
        public java.util.Map.Entry<String, V> next() {
            var key = this.keys.next();
            return NbtDeserializer.this.frame(
                    () -> this.compound.get(key),
                    () -> java.util.Map.entry(key, this.valueEndec.decode(NbtDeserializer.this)),
                    false
            );
        }
    }

    public class Struct implements Deserializer.Struct {

        private final CompoundTag compound;

        public Struct(CompoundTag compound) {
            this.compound = compound;
        }

        @Override
        public <F> @Nullable F field(String name, Endec<F> endec) {
            if (!this.compound.contains(name)) {
                throw new IllegalStateException("Field '" + name + "' was missing from serialized data, but no default value was provided");
            }

            return NbtDeserializer.this.frame(
                    () -> this.compound.get(name),
                    () -> endec.decode(NbtDeserializer.this),
                    true
            );
        }

        @Override
        public <F> @Nullable F field(String name, Endec<F> endec, @Nullable F defaultValue) {
            if (!this.compound.contains(name)) return defaultValue;
            return NbtDeserializer.this.frame(
                    () -> this.compound.get(name),
                    () -> endec.decode(NbtDeserializer.this),
                    true
            );
        }
    }
}
