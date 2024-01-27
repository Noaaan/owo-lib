package io.wispforest.uwu.items;

import io.wispforest.owo.itemgroup.OwoItemGroup;
import io.wispforest.owo.itemgroup.OwoItemSettings;
import io.wispforest.owo.ops.WorldOps;
import io.wispforest.owo.serialization.endec.BuiltInEndecs;
import io.wispforest.owo.serialization.endec.KeyedEndec;
import io.wispforest.uwu.Uwu;
import io.wispforest.uwu.text.BasedTextContent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class UwuTestStickItem extends Item {

    private static final KeyedEndec<Component> TEXT_KEY = BuiltInEndecs.TEXT.keyed("Text", Component.empty());

    public UwuTestStickItem() {
        super(new OwoItemSettings().group(Uwu.SIX_TAB_GROUP).tab(3).stacksTo(1)
                .trackUsageStat()
                .stackGenerator(OwoItemGroup.DEFAULT_STACK_GENERATOR.andThen((item, stacks) -> {
                    final var stack = new ItemStack(item);
                    stack.setHoverName(Component.literal("the stick of the test").withStyle(style -> style.withItalic(false)));
                    stacks.accept(stack);
                })));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        if (user.isShiftKeyDown()) {
            if (world.isClientSide) return InteractionResultHolder.success(user.getItemInHand(hand));

            Uwu.CHANNEL.serverHandle(user).send(new Uwu.OtherTestMessage(user.blockPosition(), "based"));

            var server = user.getServer();
            var teleportTo = world.dimension() == Level.END ? server.getLevel(Level.OVERWORLD) : server.getLevel(Level.END);

            WorldOps.teleportToWorld((ServerPlayer) user, teleportTo, new Vec3(0, 128, 0));

            return InteractionResultHolder.success(user.getItemInHand(hand));
        } else {
            if (!world.isClientSide) return InteractionResultHolder.success(user.getItemInHand(hand));

            Uwu.CHANNEL.clientHandle().send(Uwu.MESSAGE);

            Uwu.CUBE.spawn(world, user.getEyePosition().add(user.getViewVector(0).scale(3)).subtract(.5, .5, .5), null);
            user.sendSystemMessage(Component.translatable("uwu.a", "bruh"));

            return InteractionResultHolder.success(user.getItemInHand(hand));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getPlayer().isShiftKeyDown()) return InteractionResult.PASS;
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;

        final var breakStack = new ItemStack(Items.NETHERITE_PICKAXE);
        breakStack.enchant(Enchantments.BLOCK_FORTUNE, 3);
        WorldOps.breakBlockWithItem(context.getLevel(), context.getClickedPos(), breakStack);

        final var stickStack = context.getItemInHand();

        if (!stickStack.has(TEXT_KEY)) {
            stickStack.put(TEXT_KEY, Component.nullToEmpty(String.valueOf(context.getLevel().random.nextInt(1000000))));
        }

        stickStack.mutate(TEXT_KEY, text -> MutableComponent.create(new BasedTextContent("basednite, ")).append(text));

        context.getPlayer().displayClientMessage(stickStack.getTag().get(TEXT_KEY), false);

        Uwu.BREAK_BLOCK_PARTICLES.spawn(context.getLevel(), Vec3.atLowerCornerOf(context.getClickedPos()), null);

        return InteractionResult.SUCCESS;
    }
}
