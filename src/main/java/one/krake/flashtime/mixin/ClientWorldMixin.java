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
@SuppressWarnings("unused")
public abstract class ClientWorldMixin {
    /**
     * Mixin that runs before tickEntity() and blocks all calls to ticking a player entity unless the invocation is
     * from the player's timer (which sets a certain flag)
     *
     * @param entity The entity to update
     * @param _info  Callback info
     */
    @Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
    protected void onlyTickPlayerIfIAllowIt(Entity entity, CallbackInfo _info) {
        if (entity instanceof PlayerEntity && !FlashTimeState.INSTANCE.getUnlockPlayerTick()) {
            _info.cancel();
        }
    }
}
