package one.krake.flashtime.mixin;

import net.minecraft.client.render.GameRenderer;
import one.krake.flashtime.FlashTimeState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    private int ticks;

    @ModifyArg(
            method = "renderWorld",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V"),
            index = 4
    )
    // Smooth player movement
    protected float changeCameraUpdateTickDelta(float _prevTickDelta) {
        return (float) FlashTimeState.INSTANCE.getLastTickDelta();
    }

    @ModifyArg(
            method = "renderWorld",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;bobView(Lnet/minecraft/client/util/math/MatrixStack;F)V"),
            index = 1
    )
    // Smooth view bobbing
    protected float changeBobViewTickDelta(float _prevTickDelta) {
        return (float) FlashTimeState.INSTANCE.getLastTickDelta();
    }

    @ModifyArg(
            method = "renderWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/GameRenderer;renderHand(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/Camera;F)V"
            ),
            index = 2
    )
    // Smooth hand rendering
    protected float changeHandTickDelta(float _prevTickDelta) {
        return (float) FlashTimeState.INSTANCE.getLastTickDelta();
    }
}
