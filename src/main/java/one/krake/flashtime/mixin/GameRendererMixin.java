package one.krake.flashtime.mixin;

import net.minecraft.client.render.GameRenderer;
import one.krake.flashtime.FlashTimeState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GameRenderer.class)
@SuppressWarnings("unused")
public abstract class GameRendererMixin {
    @Shadow
    private int ticks;

    /**
     * Mixin that runs when renderWorld() tries to run camera.update(), and updates the tickDelta to that of the
     * player's timer, which causes smooth player movement.
     *
     * @param _prevTickDelta The previous value
     * @return The new value
     */
    @ModifyArg(
            method = "renderWorld",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V"),
            index = 4
    )
    protected float changeCameraUpdateTickDelta(float _prevTickDelta) {
        return (float) FlashTimeState.INSTANCE.getLastTickDelta();
    }

    /**
     * Mixin that runs when renderWorld() tries to run gameRenderer.bobView(), and updates the tickDelta to that of the
     * player's timer, which causes smooth view bobbing (whenever that option is on).
     *
     * @param _prevTickDelta The previous value
     * @return The new value
     */
    @ModifyArg(
            method = "renderWorld",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;bobView(Lnet/minecraft/client/util/math/MatrixStack;F)V"),
            index = 1
    )
    protected float changeBobViewTickDelta(float _prevTickDelta) {
        return (float) FlashTimeState.INSTANCE.getLastTickDelta();
    }

    /**
     * Mixin that runs when renderWorld() tries to run gameRenderer.renderHand(), and updates the tickDelta to that of the
     * player's timer, which causes smooth hand rendering (otherwise it glitches quite a bit)
     *
     * @param _prevTickDelta The previous value
     * @return The new value
     */
    @ModifyArg(
            method = "renderWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/GameRenderer;renderHand(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/Camera;F)V"
            ),
            index = 2
    )
    protected float changeHandTickDelta(float _prevTickDelta) {
        return (float) FlashTimeState.INSTANCE.getLastTickDelta();
    }
}
