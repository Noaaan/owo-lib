package io.wispforest.owo.ui.parsing;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonGrammar;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.api.SyntaxError;
import io.wispforest.owo.Owo;
import io.wispforest.owo.ops.TextOps;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UIModelLoader implements ResourceManagerReloadListener, IdentifiableResourceReloadListener {

    private static final Map<ResourceLocation, UIModel> LOADED_MODELS = new HashMap<>();

    private static final Jankson JANKSON = Jankson.builder()
            .registerSerializer(Path.class, (path, marshaller) -> JsonPrimitive.of(path.toString()))
            .registerSerializer(ResourceLocation.class, (identifier, marshaller) -> new JsonPrimitive(identifier.toString()))
            .build();

    private static final Path HOT_RELOAD_LOCATIONS_PATH = FabricLoader.getInstance().getConfigDir().resolve("owo_ui_hot_reload_locations.json5");
    private static final Map<ResourceLocation, Path> HOT_RELOAD_LOCATIONS = new HashMap<>();

    /**
     * Get the most up-to-date version of the UI model specified
     * by the given identifier. If debug mod is enabled and a hot reload
     * location has been configured by the user for this specific model,
     * a hot reload will be attempted
     *
     * @return The most up-to-date version of the requested model, or
     * the result of {@link #getPreloaded(ResourceLocation)} if the hot reload
     * fails for any reason
     */
    public static @Nullable UIModel get(ResourceLocation id) {
        if (Owo.DEBUG && HOT_RELOAD_LOCATIONS.containsKey(id)) {
            try (var stream = Files.newInputStream(HOT_RELOAD_LOCATIONS.get(id))) {
                return UIModel.load(stream);
            } catch (ParserConfigurationException | IOException | SAXException e) {
                Minecraft.getInstance().player.sendSystemMessage(TextOps.concat(Owo.PREFIX, TextOps.withFormatting("hot ui model reload failed, check the log for details")));
                Owo.LOGGER.error("Hot UI model reload failed", e);
            }
        }

        return getPreloaded(id);
    }

    /**
     * Fetch the UI model specified by the given identifier from the
     * cache created during the last resource reload
     */
    public static @Nullable UIModel getPreloaded(ResourceLocation id) {
        return LOADED_MODELS.getOrDefault(id, null);
    }

    /**
     * Set the path from which to attempt a hot reload when the UI
     * model with the given identifier is requested through {@link #get(ResourceLocation)}.
     * <p>
     * Call with a {@code null} path to clear
     */
    public static void setHotReloadPath(ResourceLocation modelId, @Nullable Path reloadPath) {
        if (reloadPath != null) {
            HOT_RELOAD_LOCATIONS.put(modelId, reloadPath);
        } else {
            HOT_RELOAD_LOCATIONS.remove(modelId);
        }

        try {
            Files.writeString(HOT_RELOAD_LOCATIONS_PATH, JANKSON.toJson(HOT_RELOAD_LOCATIONS).toJson(JsonGrammar.JSON5));
        } catch (IOException e) {
            Owo.LOGGER.warn("Could not save hot reload locations", e);
        }
    }

    public static @Nullable Path getHotReloadPath(ResourceLocation modelId) {
        return HOT_RELOAD_LOCATIONS.get(modelId);
    }

    public static Set<ResourceLocation> allLoadedModels() {
        return Collections.unmodifiableSet(LOADED_MODELS.keySet());
    }

    @Override
    public ResourceLocation getFabricId() {
        return new ResourceLocation("owo", "ui-model-loader");
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        LOADED_MODELS.clear();

        manager.listResources("owo_ui", identifier -> identifier.getPath().endsWith(".xml")).forEach((resourceId, resource) -> {
            try {
                var modelId = new ResourceLocation(
                        resourceId.getNamespace(),
                        resourceId.getPath().substring(7, resourceId.getPath().length() - 4)
                );

                LOADED_MODELS.put(modelId, UIModel.load(resource.open()));
            } catch (ParserConfigurationException | IOException | SAXException e) {
                Owo.LOGGER.error("Could not parse UI model {}", resourceId, e);
            }
        });
    }

    static {
        if (Owo.DEBUG && Files.exists(HOT_RELOAD_LOCATIONS_PATH)) {
            try (var stream = Files.newInputStream(HOT_RELOAD_LOCATIONS_PATH)) {
                var associations = JANKSON.load(stream);
                associations.forEach((key, value) -> {
                    if (!(value instanceof JsonPrimitive primitive)) return;
                    HOT_RELOAD_LOCATIONS.put(new ResourceLocation(key), Path.of(primitive.asString()));
                });
            } catch (IOException | SyntaxError ignored) {}
        }
    }
}
