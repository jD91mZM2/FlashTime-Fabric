package one.krake.flashtime.mixin;

import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.ServerTask;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Util;
import net.minecraft.util.profiler.DisableableProfiler;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import one.krake.flashtime.FlashTimeState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.function.BooleanSupplier;

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
