package one.krake.flashtime.mixin;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import one.krake.flashtime.FlashTimeState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
    protected void onlyTickPlayerIAllowIt(Entity entity, CallbackInfo _info) {
        if (entity instanceof PlayerEntity && !FlashTimeState.INSTANCE.getUnlockPlayerTick()) {
            _info.cancel();
        }
    }
}
