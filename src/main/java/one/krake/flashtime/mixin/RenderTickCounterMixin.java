package one.krake.flashtime.mixin;

import net.minecraft.client.render.RenderTickCounter;
import one.krake.flashtime.IRenderTickCounterAdditions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderTickCounter.class)
@SuppressWarnings("unused")
public abstract class RenderTickCounterMixin implements IRenderTickCounterAdditions {
    @Shadow
    @Final
    @Mutable
    private float tickTime;

    /**
     * Updates the tick time similarly to how constructing a new RenderTickCounter with a different tick speed would.
     *
     * @param tps The desired amount of ticks per second
     */
    @Override
    public void setTps(float tps) {
        this.tickTime = 1000.0F / tps;
    }
}
