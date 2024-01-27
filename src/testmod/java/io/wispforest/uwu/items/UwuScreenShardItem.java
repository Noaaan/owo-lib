package io.wispforest.uwu.items;

import io.wispforest.uwu.EpicScreenHandler;
import io.wispforest.uwu.client.SelectUwuScreenScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class UwuScreenShardItem extends Item {

    public UwuScreenShardItem() {
        super(new Properties().rarity(Rarity.UNCOMMON));
    }

    @Override
    @Environment(EnvType.CLIENT)
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        if (user.isShiftKeyDown()) {
            if (world.isClientSide) Minecraft.getInstance().setScreen(new SelectUwuScreenScreen());
        } else if (!world.isClientSide) {
            user.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("bruh momento");
                }

                @Override
                public @NotNull AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
                    return new EpicScreenHandler(syncId, inv, ContainerLevelAccess.create(world, player.blockPosition()));
                }
            });
        }
        return InteractionResultHolder.pass(user.getItemInHand(hand));
    }
}
