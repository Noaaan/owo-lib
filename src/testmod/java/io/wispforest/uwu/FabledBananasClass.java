package io.wispforest.uwu;

import com.google.gson.*;
import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.serialization.endec.BuiltInEndecs;
import io.wispforest.owo.serialization.endec.StructEndecBuilder;
import io.wispforest.owo.serialization.format.json.JsonDeserializer;
import io.wispforest.owo.serialization.format.json.JsonSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import java.util.List;
import java.util.Map;

public class FabledBananasClass {

    public static final Endec<FabledBananasClass> ENDEC = StructEndecBuilder.of(
            Endec.INT.fieldOf("banana_amount", FabledBananasClass::bananaAmount),
            BuiltInEndecs.ofRegistry(BuiltInRegistries.ITEM).fieldOf("banana_item", FabledBananasClass::bananaItem),
            BuiltInEndecs.BLOCK_POS.listOf().fieldOf("banana_positions", FabledBananasClass::bananaPositions),
            FabledBananasClass::new
    );

    private final int bananaAmount;
    private final Item bananaItem;
    private final List<BlockPos> bananaPositions;

    public FabledBananasClass(int bananaAmount, Item bananaItem, List<BlockPos> bananaPositions) {
        this.bananaAmount = bananaAmount;
        this.bananaItem = bananaItem;
        this.bananaPositions = bananaPositions;
    }

    public int bananaAmount() {return this.bananaAmount;}
    public Item bananaItem() {return this.bananaItem;}
    public List<BlockPos> bananaPositions() {return this.bananaPositions;}

    public static void main(String[] args) {
        var pos = new BlockPos(1, 2, 3);
        JsonElement result = BuiltInEndecs.BLOCK_POS.encodeFully(JsonSerializer::of, pos);

        System.out.println(result);
        BlockPos decoded = BuiltInEndecs.BLOCK_POS.decodeFully(JsonDeserializer::of, result);


        Endec<Map<ResourceLocation, Integer>> endec = Endec.map(ResourceLocation::toString, ResourceLocation::new, Endec.INT);
        System.out.println(endec.encodeFully(JsonSerializer::of, Map.of(new ResourceLocation("a"), 6, new ResourceLocation("b"), 9)).toString());
        System.out.println(endec.decodeFully(JsonDeserializer::of, new Gson().fromJson("{\"a:b\":24,\"c\":17}", JsonObject.class)));

        Endec<Map<BlockPos, ResourceLocation>> mappy = Endec.map(BuiltInEndecs.BLOCK_POS, BuiltInEndecs.IDENTIFIER);
        System.out.println(mappy.encodeFully(JsonSerializer::of, Map.of(BlockPos.ZERO, new ResourceLocation("a"), new BlockPos(69, 420, 489), new ResourceLocation("bruh:l"))).toString());
        System.out.println(mappy.decodeFully(JsonDeserializer::of, new Gson().fromJson("[{\"k\":[69,420,489],\"v\":\"bruh:l\"},{\"k\":[0,0,0],\"v\":\"minecraft:a\"}]", JsonArray.class)));
    }
}
