package one.krake.flashtime.mixin;

import net.minecraft.client.render.RenderTickCounter;
import one.krake.flashtime.IRenderTickCounterAdditions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderTickCounter.class)
public abstract class RenderTickCounterMixin implements IRenderTickCounterAdditions {
    @Shadow
    @Final
    private float tickTime;

    @Override
    public void setTps(float tps) {
        this.tickTime = 1000.0F / tps;
    }
}
