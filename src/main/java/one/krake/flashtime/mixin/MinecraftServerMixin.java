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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.function.BooleanSupplier;

// Mixins HAVE to be written in java due to constraints in the mixin system.

// Priority is marked as being most important ever. I mean, this function is the main star of the show,
// if it can't override this function then there's no reason to use the FlashTime mod at all.
@Mixin(value = MinecraftServer.class, priority = 0)
public abstract class MinecraftServerMixin extends ReentrantThreadExecutor<ServerTask> {
    @Shadow
    private long timeReference;
    @Shadow
    private boolean stopped;
    @Shadow
    private boolean profilerStartQueued;
    @Shadow
    private ServerMetadata metadata;
    @Shadow
    private static Logger LOGGER;
    @Shadow
    private volatile boolean loading;

    @Shadow
    protected abstract boolean setupServer() throws IOException;
    @Shadow
    public boolean isRunning() { return false; }
    @Shadow
    public String getServerMotd() { return null; }
    @Shadow
    public void setFavicon(ServerMetadata metadata) {}
    @Shadow
    public DisableableProfiler getProfiler() { return null; }
    @Shadow
    protected void tick(BooleanSupplier booleanSupplier) {}
    @Shadow
    protected void shutdown() {}
    @Shadow
    protected void exit() {}

    private long previousTimeReference;
    private boolean keepTicking;
    private long timeUntilNextTick;

    // Java complains if I extend without a constructor
    private MinecraftServerMixin() {
        super(null);
    }

    /*
    THE MAIN STAR

    This bad boy is a replacement of MinecraftServer's normal `run` function, that allows for a custom tick speed.
    IMO the default is pretty badly written in terms of hardcoding. This does not address that so much as it just uses
    a variable tick speed.

    The reason this is an override instead of a nice callback injection is that there are just a lot of references
    to the hardcoded value 50L. In the future I should definitely look into using callback injections, although I'm not
    entirely sure those can even achieve what I want :(

    TODO: Potentially useful injectors:
    - https://github.com/SpongePowered/Mixin/wiki/Injection-Point-Reference#beforeconstant
    -  http://jenkins.liteloader.com/view/Other/job/Mixin/javadoc/index.html?org/spongepowered/asm/mixin/injection/ModifyConstant.html
     */
    public void run() {
        try {
            if (this.setupServer()) {
                this.timeReference = Util.getMeasuringTimeMs();
                // MIXIN: Replaced `this.motd` with `this.getServerMotd()` because it's public
                this.metadata.setDescription(new LiteralText(this.getServerMotd()));
                this.metadata.setVersion(new ServerMetadata.Version(SharedConstants.getGameVersion().getName(), SharedConstants.getGameVersion().getProtocolVersion()));
                this.setFavicon(this.metadata);

                // MIXIN: Replaced `this.running` with `this.isRunning()` because it's public
                while(this.isRunning()) {
                    long l = Util.getMeasuringTimeMs() - this.timeReference;
                    // MIXIN: Replaced obfuscated `field_4557` with copy: previousTimeReference
                    if (l > 2000L && this.timeReference - this.previousTimeReference >= 15000L) {
                        // MIXIN: 50L -> FlashTimeState.INSTANCE.getTickDelayMillis()
                        long m = l / FlashTimeState.INSTANCE.getTickDelayMillis();
                        LOGGER.warn("Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind", l, m);
                        // MIXIN: 50L -> FlashTimeState.INSTANCE.getTickDelayMillis()
                        this.timeReference += m * FlashTimeState.INSTANCE.getTickDelayMillis();
                        this.previousTimeReference = this.timeReference;
                    }

                    // MIXIN: 50L -> FlashTimeState.INSTANCE.getTickDelayMillis()
                    this.timeReference += FlashTimeState.INSTANCE.getTickDelayMillis();
                    if (this.profilerStartQueued) {
                        this.profilerStartQueued = false;
                        // MIXIN: Replaced `this.profiler` with `this.getProfiler`
                        this.getProfiler().getController().enable();
                    }

                    this.getProfiler().startTick();
                    this.getProfiler().push("tick");
                    this.tick(this::shouldKeepTicking);
                    this.getProfiler().swap("nextTickWait");
                    // MIXIN: More obfuscated fields replaced
                    this.keepTicking = true;
                    // MIXIN: 50L -> FlashTimeState.INSTANCE.getTickDelayMillis()
                    this.timeUntilNextTick = Math.max(Util.getMeasuringTimeMs() + FlashTimeState.INSTANCE.getTickDelayMillis(), this.timeReference);
                    // MIXIN: Copied function with obfuscated name
                    this.weirdVersionOfRunTasks();
                    //
                    this.getProfiler().pop();
                    this.getProfiler().endTick();
                    this.loading = true;
                }
            } else {
                // MIXIN: Removed setCrashReport(null) line
            }
        } catch (Throwable var44) {
            // MIXIN: Removing crash report stuff
            LOGGER.error("Encountered an unexpected exception", var44);
        } finally {
            try {
                this.stopped = true;
                this.shutdown();
            } catch (Throwable var42) {
                LOGGER.error("Exception stopping the server", var42);
            } finally {
                this.exit();
            }

        }

    }

    protected void weirdVersionOfRunTasks() {
        this.runTasks();
        this.runTasks(() -> {
            return !this.shouldKeepTicking();
        });
    }

    private boolean shouldKeepTicking() {
        return this.hasRunningTasks() || Util.getMeasuringTimeMs() < (this.keepTicking ? this.timeUntilNextTick : this.timeReference);
    }

    @Inject(method = "runTask", at = @At("RETURN"))
    private void onRunTask(CallbackInfoReturnable<Boolean> info) {
        this.keepTicking = info.getReturnValue();
    }
}
