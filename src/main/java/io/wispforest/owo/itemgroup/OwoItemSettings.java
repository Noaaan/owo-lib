package io.wispforest.owo.itemgroup;

import net.fabricmc.fabric.api.item.v1.CustomDamageHandler;
import net.fabricmc.fabric.api.item.v1.EquipmentSlotProvider;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public class OwoItemSettings extends FabricItemSettings {

    @Nullable
    private OwoItemGroup group = null;
    private int tab = 0;
    private BiConsumer<Item, CreativeModeTab.Output> stackGenerator = OwoItemGroup.DEFAULT_STACK_GENERATOR;
    private boolean trackUsageStat = false;

    public OwoItemSettings group(ItemGroupReference ref) {
        this.group = ref.group();
        this.tab = ref.tab();
        return this;
    }

    /**
     * @param group The item group this item should appear in
     */
    public OwoItemSettings group(OwoItemGroup group) {
        this.group = group;
        return this;
    }

    public OwoItemGroup group() {
        return this.group;
    }

    public OwoItemSettings tab(int tab) {
        this.tab = tab;
        return this;
    }

    public int tab() {
        return this.tab;
    }

    /**
     * @param generator The function this item uses for creating stacks in the
     *                  {@link OwoItemGroup} it is in, by default this will be {@link OwoItemGroup#DEFAULT_STACK_GENERATOR}
     */
    public OwoItemSettings stackGenerator(BiConsumer<Item, CreativeModeTab.Output> generator) {
        this.stackGenerator = generator;
        return this;
    }

    public BiConsumer<Item, CreativeModeTab.Output> stackGenerator() {
        return this.stackGenerator;
    }

    /**
     * Automatically increment {@link net.minecraft.stats.Stats#ITEM_USED}
     * for this item every time {@link Item#use(Level, Player, InteractionHand)}
     * returns an accepted result
     */
    public OwoItemSettings trackUsageStat() {
        this.trackUsageStat = true;
        return this;
    }

    public boolean shouldTrackUsageStat() {
        return this.trackUsageStat;
    }

    @Override
    public OwoItemSettings equipmentSlot(EquipmentSlotProvider equipmentSlotProvider) {
        return (OwoItemSettings) super.equipmentSlot(equipmentSlotProvider);
    }

    @Override
    public OwoItemSettings customDamage(CustomDamageHandler handler) {
        return (OwoItemSettings) super.customDamage(handler);
    }

    @Override
    public OwoItemSettings food(FoodProperties foodComponent) {
        return (OwoItemSettings) super.food(foodComponent);
    }

    @Override
    public OwoItemSettings stacksTo(int maxCount) {
        return (OwoItemSettings) super.stacksTo(maxCount);
    }

    @Override
    public OwoItemSettings defaultDurability(int maxDamage) {
        return (OwoItemSettings) super.defaultDurability(maxDamage);
    }

    @Override
    public OwoItemSettings durability(int maxDamage) {
        return (OwoItemSettings) super.durability(maxDamage);
    }

    @Override
    public OwoItemSettings craftRemainder(Item recipeRemainder) {
        return (OwoItemSettings) super.craftRemainder(recipeRemainder);
    }

    @Override
    public OwoItemSettings rarity(Rarity rarity) {
        return (OwoItemSettings) super.rarity(rarity);
    }

    @Override
    public OwoItemSettings fireResistant() {
        return (OwoItemSettings) super.fireResistant();
    }

}
