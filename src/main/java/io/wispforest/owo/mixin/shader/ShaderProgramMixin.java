package io.wispforest.owo.mixin.shader;


import io.wispforest.owo.shader.GlProgram;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ShaderInstance.class)
public class ShaderProgramMixin {

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/ResourceLocation;<init>(Ljava/lang/String;)V"), require = 0)
    private String fixIdentifier(String id) {
        if (!((Object) this instanceof GlProgram.OwoShaderProgram)) return id;

        var splitName = id.split(":");
        if (splitName.length != 2 || !splitName[0].startsWith("shaders/core/")) return id;

        return splitName[0].replace("shaders/core/", "") + ":" + "shaders/core/" + splitName[1];
    }

}
