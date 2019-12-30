package one.krake.flashtime.mixin;

import net.minecraft.server.MinecraftServer;
import one.krake.flashtime.FlashTimeState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

// Mixins HAVE to be written in java due to constraints in the mixin system.

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    // Change the server's hardcoded tick time
    @ModifyConstant(method = "run")
    public long onRun(long constant) {
        if (constant == 50) {
            return 1000 / FlashTimeState.INSTANCE.getTicksPerSecond();
        } else {
            return constant;
        }
    }
}
