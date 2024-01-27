package io.wispforest.owo.moddata;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

/**
 * A class that can accept some JSON data loaded from a subdirectory
 * of all other mods' {@code data} directories when instructed to using
 * {@link ModDataLoader#load(ModDataConsumer)}
 */
public interface ModDataConsumer {

    /**
     * The {@code data} subdirectory to search. For example {@code items} would
     * mean {@code .../data/{modid}/items/...}
     *
     * @return The subdirectory to load from
     */
    String getDataSubdirectory();

    /**
     * This method should process the loaded data
     *
     * @param object The .json files parsed into {@code JsonObject}s
     */
    void acceptParsedFile(ResourceLocation id, JsonObject object);

}
