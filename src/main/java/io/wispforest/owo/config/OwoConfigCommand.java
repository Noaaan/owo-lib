package io.wispforest.owo.config;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.wispforest.owo.Owo;
import io.wispforest.owo.config.ui.ConfigScreen;
import io.wispforest.owo.ops.TextOps;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@ApiStatus.Internal
public class OwoConfigCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext access) {
        dispatcher.register(ClientCommandManager.literal("owo-config")
                .then(ClientCommandManager.argument("config_id", new ConfigScreenArgumentType())
                        .executes(context -> {
                            var screen = context.getArgument("config_id", ConfigScreen.class);
                            Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(screen));
                            return 0;
                        })));
    }

    private static class ConfigScreenArgumentType implements ArgumentType<ConfigScreen> {

        private static final SimpleCommandExceptionType NO_SUCH_CONFIG_SCREEN = new SimpleCommandExceptionType(
                TextOps.concat(Owo.PREFIX, Component.literal("no config screen with that id"))
        );

        @Override
        public ConfigScreen parse(StringReader reader) throws CommandSyntaxException {
            var provider = ConfigScreen.getProvider(reader.readString());
            if (provider == null) throw NO_SUCH_CONFIG_SCREEN.create();

            return provider.apply(null);
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            var configNames = new ArrayList<String>();
            ConfigScreen.forEachProvider((s, screenFunction) -> configNames.add(s));
            return SharedSuggestionProvider.suggest(configNames, builder);
        }
    }
}
