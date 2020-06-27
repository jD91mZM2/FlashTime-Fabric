package one.krake.flashtime.mixin;

import net.minecraft.server.MinecraftServer;
import one.krake.flashtime.FlashTimeState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

// Mixins HAVE to be written in java due to constraints in the mixin system.

@Mixin(MinecraftServer.class)
@SuppressWarnings("unused")
public abstract class MinecraftServerMixin {
    /**
     * Mixin that changes the server's hardcoded tick time. All "50L" constants are replaced with a dynamic one.
     *
     * @param constant The constant to be inspected
     * @return The new constant
     */
    @ModifyConstant(method = "method_29741")
    public long onRun(long constant) {
        if (constant == 50) {
            return (long) (1000 / FlashTimeState.INSTANCE.getWorldTimer().getTps());
        } else {
            return constant;
        }
    }
}
