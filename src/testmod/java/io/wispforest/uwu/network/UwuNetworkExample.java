package io.wispforest.uwu.network;

import io.wispforest.owo.network.OwoNetChannel;
import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.serialization.StructEndec;
import io.wispforest.owo.serialization.endec.RecordEndec;
import io.wispforest.owo.serialization.endec.ReflectiveEndecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import java.util.HashMap;
import java.util.Map;

public class UwuNetworkExample {
    public static final Map<String, StructEndec<? extends DispatchedInterface>> REGISTRY = new HashMap<>();
    public static final OwoNetChannel CHANNEL = OwoNetChannel.create(new ResourceLocation("uwu", "main"));

    public static void init() {
        REGISTRY.put("one", RecordEndec.create(DispatchedSubclassOne.class));
        REGISTRY.put("two", RecordEndec.create(DispatchedSubclassTwo.class));

        ReflectiveEndecBuilder.register(Endec.dispatchedStruct(REGISTRY::get, DispatchedInterface::getName, Endec.STRING), DispatchedInterface.class);

        CHANNEL.registerClientbound(StringPacket.class, (message, access) -> {
            access.player().displayClientMessage(Component.literal(message.value()), false);
        });

        CHANNEL.registerServerbound(KeycodePacket.class, (message, access) -> {
            CHANNEL.serverHandle(access.player()).send(new StringPacket("Key " + message.key() + " pressed"));
        });

        CHANNEL.registerServerbound(MaldingPacket.class, (message, access) -> {
            access.player().displayClientMessage(Component.literal(message.toString()), false);
        });
    }

    @Environment(EnvType.CLIENT)
    public static final class Client {
        public static final KeyMapping NETWORK_TEST = new KeyMapping("key.uwu.network_test", GLFW.GLFW_KEY_U, "misc");

        public static void init() {
            KeyBindingHelper.registerKeyBinding(NETWORK_TEST);
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                while (NETWORK_TEST.consumeClick()) {
                    CHANNEL.clientHandle().send(new KeycodePacket(KeyBindingHelper.getBoundKeyOf(NETWORK_TEST).getValue()));

                    CHANNEL.clientHandle().send(new MaldingPacket(new DispatchedSubclassOne("base")));
                    CHANNEL.clientHandle().send(new MaldingPacket(new DispatchedSubclassTwo(20)));
                }
            });
        }
    }
}
