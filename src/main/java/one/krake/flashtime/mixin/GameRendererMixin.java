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
    protected float onCameraUpdateTickDelta(float _prevTickDelta) {
        return (float) FlashTimeState.INSTANCE.getLastTickDelta();
    }
}
