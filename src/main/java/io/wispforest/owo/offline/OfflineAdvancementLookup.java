package io.wispforest.owo.offline;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import io.wispforest.owo.Owo;
import io.wispforest.owo.mixin.offline.AdvancementProgressAccessor;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/**
 * Allows retrieving and editing the saved
 * advancement data of a player
 * <p>
 * <b>This only works while the given player is offline</b>
 *
 * @author BasiqueEvangelist
 */
public final class OfflineAdvancementLookup {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private OfflineAdvancementLookup() {}

    public static final Codec<Map<ResourceLocation, AdvancementProgress>> CODEC = DataFixTypes.ADVANCEMENTS.wrapCodec(
        Codec.unboundedMap(ResourceLocation.CODEC, AdvancementProgress.CODEC),
        DataFixers.getDataFixer(),
        1343
    );

    /**
     * Saves the given advancement state
     * for the given player to disk
     *
     * @param player The player to modify
     * @param map    The advancement state to save
     */
    public static void put(UUID player, Map<ResourceLocation, AdvancementProgress> map) {
        DataSavedEvents.ADVANCEMENTS.invoker().onSaved(player, map);

        try {
            Path advancementsPath = Owo.currentServer().getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR);
            Path advancementPath = advancementsPath.resolve(player.toString() + ".json");
            JsonElement saved = Util.getOrThrow(CODEC.encodeStart(JsonOps.INSTANCE, map), IllegalStateException::new);

            try (BufferedWriter bw = Files.newBufferedWriter(advancementPath)) {
                GSON.toJson(saved, bw);
            }

        } catch (IOException e) {
            Owo.LOGGER.error("Couldn't save advancements of offline player {}", player, e);
            throw new RuntimeException(e);
        }
    }


    /**
     * Loads the advancement state
     * of the given player from disk
     *
     * @param player The player to query
     * @return The saved advancement data, or {@code null} if none is saved
     */
    public static @Nullable Map<ResourceLocation, AdvancementProgress> get(UUID player) {
        try {
            Path advancementsPath = Owo.currentServer().getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR);

            if (!Files.exists(advancementsPath))
                return null;

            Path advancementFile = advancementsPath.resolve(player + ".json");

            if (!Files.exists(advancementFile))
                return null;

            Map<ResourceLocation, AdvancementProgress> parsedMap;

            try (InputStream s = Files.newInputStream(advancementFile);
                 InputStreamReader streamReader = new InputStreamReader(s);
                 JsonReader reader = new JsonReader(streamReader)) {
                reader.setLenient(false);
                JsonElement jsonElement = Streams.parse(reader);
                parsedMap = Util.getOrThrow(CODEC.parse(JsonOps.INSTANCE, jsonElement), JsonParseException::new);
            }

            for (Map.Entry<ResourceLocation, AdvancementProgress> entry : parsedMap.entrySet()) {
                var requirements = ((AdvancementProgressAccessor) entry.getValue()).getRequirements();

                if (requirements.size() == 0) {
                    AdvancementHolder adv = Owo.currentServer().getAdvancements().get(entry.getKey());

                    if (adv != null) {
                        ((AdvancementProgressAccessor) entry.getValue()).setRequirements(adv.value().requirements());
                    }
                }
            }

            return parsedMap;
        } catch (IOException e) {
            Owo.LOGGER.error("Couldn't get advancements for offline player {}", player, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Edits the saved advancement state of the given player
     * with the given editing function
     *
     * @param player The player to target
     * @param editor The function to apply to the advancement state
     */
    public static void edit(UUID player, Consumer<OfflineAdvancementState> editor) {
        Map<ResourceLocation, AdvancementProgress> advancementData = get(player);
        if (advancementData == null) advancementData = new HashMap<>();

        var transaction = new OfflineAdvancementState(advancementData);
        editor.accept(transaction);
        put(player, transaction.advancementData());
    }

    /**
     * @return The UUID of every player that has saved advancements
     */
    public static List<UUID> savedPlayers() {
        Path advancementsPath = Owo.currentServer().getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR);

        if (!Files.isDirectory(advancementsPath))
            return Collections.emptyList();

        List<UUID> list = new ArrayList<>();

        try {
            Iterator<Path> iterator = Files.list(advancementsPath).iterator();
            while (iterator.hasNext()) {
                Path savedPlayerFile = iterator.next();

                if (Files.isDirectory(savedPlayerFile) || !savedPlayerFile.toString().endsWith(".json")) {
                    continue;
                }

                try {
                    String filename = savedPlayerFile.getFileName().toString();
                    String uuidStr = filename.substring(0, filename.lastIndexOf('.'));
                    UUID uuid = UUID.fromString(uuidStr);
                    list.add(uuid);
                } catch (IllegalArgumentException iae) {
                    Owo.LOGGER.error("Encountered invalid UUID in advancements directory", iae);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return list;
    }
}
