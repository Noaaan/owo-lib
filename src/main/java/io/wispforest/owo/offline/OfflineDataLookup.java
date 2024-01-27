package io.wispforest.owo.offline;

import io.wispforest.owo.Owo;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

/**
 * Allows retrieving and editing the saved
 * NBT data of a player
 * <p>
 * <b>This only works while the given player is offline</b>
 *
 * @author BasiqueEvangelist
 */
public final class OfflineDataLookup {

    private OfflineDataLookup() {}

    /**
     * Saves the given NBT tag
     * for the given player to disk
     *
     * @param player The player to modify
     * @param nbt    The data to save
     */
    public static void put(UUID player, CompoundTag nbt) {
        DataSavedEvents.PLAYER_DATA.invoker().onSaved(player, nbt);

        try {
            var savedPlayersPath = Owo.currentServer().getWorldPath(LevelResource.PLAYER_DATA_DIR);
            var file = Files.createTempFile(savedPlayersPath, player.toString() + "-", ".dat");
            NbtIo.writeCompressed(nbt, file);
            var newDataFile = savedPlayersPath.resolve(player + ".dat");
            var oldDataFile = savedPlayersPath.resolve(player + ".dat_old");
            Util.safeReplaceFile(newDataFile, file, oldDataFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads the playerdata
     * of the given player from disk
     *
     * @param player The player to query
     * @return The saved playerdata, or {@code null} if none is saved
     */
    public static @Nullable CompoundTag get(UUID player) {
        try {
            Path savedPlayersPath = Owo.currentServer().getWorldPath(LevelResource.PLAYER_DATA_DIR);
            Path savedDataPath = savedPlayersPath.resolve(player.toString() + ".dat");
            CompoundTag rawNbt = NbtIo.readCompressed(savedDataPath, NbtAccounter.unlimitedHeap());
            int dataVersion = rawNbt.contains("DataVersion", 3) ? rawNbt.getInt("DataVersion") : -1;
            return DataFixTypes.PLAYER.updateToCurrentVersion(DataFixers.getDataFixer(), rawNbt, dataVersion);
        } catch (IOException e) {
            Owo.LOGGER.error("Couldn't get player data for offline player {}", player, e);
            return null;
        }
    }

    /**
     * Edits the saved data of the given player
     * with the given editing function
     *
     * @param player The player to target
     * @param editor The function to apply to the saved data
     */
    public static void edit(UUID player, Function<CompoundTag, CompoundTag> editor) {
        put(player, editor.apply(get(player)));
    }

    /**
     * @return The UUID of every player that has saved playerdata
     */
    public static List<UUID> savedPlayers() {
        List<UUID> list = new ArrayList<>();
        Path savedPlayersPath = Owo.currentServer().getWorldPath(LevelResource.PLAYER_DATA_DIR);

        if (!Files.isDirectory(savedPlayersPath))
            return Collections.emptyList();

        try {
            Iterator<Path> iterator = Files.list(savedPlayersPath).iterator();
            while (iterator.hasNext()) {
                Path savedPlayerFile = iterator.next();

                if (Files.isDirectory(savedPlayerFile) || !savedPlayerFile.toString().endsWith(".dat")) {
                    continue;
                }

                try {
                    String filename = savedPlayerFile.getFileName().toString();
                    String uuidStr = filename.substring(0, filename.lastIndexOf('.'));
                    UUID uuid = UUID.fromString(uuidStr);
                    list.add(uuid);
                } catch (IllegalArgumentException iae) {
                    Owo.LOGGER.error("Encountered invalid UUID in playerdata directory", iae);
                }
            }
        } catch (IOException e) {
            Owo.LOGGER.error("Couldn't list offline player UUIDs", e);
            throw new RuntimeException(e);
        }

        return list;
    }
}
