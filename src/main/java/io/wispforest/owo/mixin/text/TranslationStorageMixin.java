package io.wispforest.owo.mixin.text;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import io.wispforest.owo.text.LanguageAccess;
import io.wispforest.owo.text.TextLanguage;
import io.wispforest.owo.util.KawaiiUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;

@Mixin(ClientLanguage.class)
public class TranslationStorageMixin implements TextLanguage {

    @Mutable
    @Shadow
    @Final
    private Map<String, String> storage;

    private static Map<String, Component> owo$buildingTextMap;

    private Map<String, Component> owo$textMap;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void kawaii(Map<String, String> storage, boolean rightToLeft, CallbackInfo ci) {
        if (!Objects.equals(System.getProperty("owo.uwu"), "yes please")) return;

        var builder = ImmutableMap.<String, String>builder();
        storage.forEach((s, s2) -> builder.put(s, KawaiiUtil.uwuify(s2)));
        this.storage = builder.build();
    }

    @Inject(method = "loadFrom", at = @At("HEAD"))
    private static void initTextMap(ResourceManager resourceManager, List<LanguageInfo> definitions, boolean leftToRight, CallbackInfoReturnable<ClientLanguage> cir) {
        owo$buildingTextMap = new HashMap<>();
        LanguageAccess.textConsumer = owo$buildingTextMap::put;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(Map<String, String> storage, boolean rightToLeft, CallbackInfo ci) {
        this.owo$textMap = owo$buildingTextMap;
        owo$buildingTextMap = null;
    }

    @Inject(method = "has", at = @At("HEAD"), cancellable = true)
    private void hasTranslation(String key, CallbackInfoReturnable<Boolean> cir) {
        if (this.owo$textMap.containsKey(key))
            cir.setReturnValue(true);
    }

    @Inject(method = "getOrDefault", at = @At("HEAD"), cancellable = true)
    private void get(String key, String fallback, CallbackInfoReturnable<String> cir) {
        if (this.owo$textMap.containsKey(key))
            cir.setReturnValue(this.owo$textMap.get(key).getString());
    }

    @Override
    public Component getText(String key) {
        return this.owo$textMap.get(key);
    }
}
