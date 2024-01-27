package io.wispforest.owo.serialization.format.nbt;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.wispforest.owo.serialization.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import java.io.IOException;

public final class NbtEndec implements Endec<Tag> {

    public static final Endec<Tag> ELEMENT = new NbtEndec();
    public static final Endec<CompoundTag> COMPOUND = new NbtEndec().xmap(CompoundTag.class::cast, compound -> compound);

    private NbtEndec() {}

    @Override
    public void encode(Serializer<?> serializer, Tag value) {
        if (serializer.attributes().contains(SerializationAttribute.SELF_DESCRIBING)) {
            NbtDeserializer.of(value).readAny(serializer);
            return;
        }

        try {
            var output = ByteStreams.newDataOutput();
            NbtIo.writeAnyTag(value, output);

            serializer.writeBytes(output.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode binary NBT in NbtEndec", e);
        }
    }

    @Override
    public Tag decode(Deserializer<?> deserializer) {
        if (deserializer instanceof SelfDescribedDeserializer<?> selfDescribedDeserializer) {
            var nbt = NbtSerializer.of();
            selfDescribedDeserializer.readAny(nbt);

            return nbt.result();
        }

        try {
            return NbtIo.readAnyTag(ByteStreams.newDataInput(deserializer.readBytes()), NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse binary NBT in NbtEndec", e);
        }
    }
}
