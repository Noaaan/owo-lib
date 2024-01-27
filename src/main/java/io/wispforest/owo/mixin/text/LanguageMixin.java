package io.wispforest.owo.mixin.text;

import com.google.gson.JsonElement;
import io.wispforest.owo.text.LanguageAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.InputStream;
import java.util.function.BiConsumer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.GsonHelper;

@Mixin(Language.class)
public class LanguageMixin {

    @Unique private static boolean skipNext;

    @Redirect(method = "loadFromJson", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/GsonHelper;convertToString(Lcom/google/gson/JsonElement;Ljava/lang/String;)Ljava/lang/String;"))
    private static String skipIfObjectOrArray(JsonElement el, String str, InputStream inputStream, BiConsumer<String, String> entryConsumer) {
        if (!el.isJsonPrimitive() && LanguageAccess.textConsumer != null) {
            skipNext = true;

            MutableComponent text = Component.Serializer.fromJson(el);
            LanguageAccess.textConsumer.accept(str, text);

            return "";
        } else if (el.isJsonPrimitive()) {
            skipNext = false;
            return GsonHelper.convertToString(el, str);
        } else {
            skipNext = true;
            return "";
        }
    }

    @Redirect(method = "loadFromJson", at = @At(value = "INVOKE", target = "Ljava/util/function/BiConsumer;accept(Ljava/lang/Object;Ljava/lang/Object;)V"))
    private static void doSkip(BiConsumer<Object, Object> biConsumer, Object t, Object u) {
        if (!skipNext)
            biConsumer.accept(t, u);
    }
}
