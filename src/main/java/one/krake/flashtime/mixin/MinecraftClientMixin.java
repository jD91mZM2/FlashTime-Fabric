package one.krake.flashtime.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SleepingChatScreen;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.sound.MusicTracker;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.tutorial.TutorialManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.ClientConnection;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.Difficulty;
import one.krake.flashtime.FlashTimeState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.stream.IntStream;

// You can find all type names at https://asm.ow2.io/asm4-guide.pdf, under "Type descriptors". You're welcome.

@Mixin(MinecraftClient.class)
@SuppressWarnings("unused")
public abstract class MinecraftClientMixin {
    private RenderTickCounter playerTickCounter = FlashTimeState.INSTANCE.getPlayerTimer().getTimer();

    @Shadow
    @Final
    private RenderTickCounter renderTickCounter;

    @Shadow
    @Final
    public GameRenderer gameRenderer;
    @Shadow
    public ClientWorld world;
    @Shadow
    @Final
    public InGameHud inGameHud;
    @Shadow
    public HitResult crosshairTarget;
    @Shadow
    public ClientPlayerInteractionManager interactionManager;
    @Shadow
    public Screen currentScreen;
    @Shadow
    public ClientPlayerEntity player;
    @Shadow
    @Final
    public GameOptions options;
    @Shadow
    @Final
    public WorldRenderer worldRenderer;
    @Shadow
    @Final
    public ParticleManager particleManager;
    @Shadow
    @Final
    public Keyboard keyboard;
    @Shadow
    protected int attackCooldown;
    @Shadow
    private boolean paused;
    @Shadow
    private int itemUseCooldown;
    @Shadow
    private float pausedTickDelta;
    @Shadow
    @Final
    private MusicTracker musicTracker;
    @Shadow
    @Final
    private SoundManager soundManager;
    @Shadow
    private ClientConnection clientConnection;

    @Shadow
    public Profiler getProfiler() {
        return null;
    }

    @Shadow
    public TutorialManager getTutorialManager() {
        return null;
    }

    @Shadow
    public TextureManager getTextureManager() {
        return null;
    }

    @Shadow
    public Overlay getOverlay() {
        return null;
    }

    @Shadow
    public void openScreen(Screen screen) {
    }

    @Shadow
    private void handleInputEvents() {
    }

    /**
     * Mixin that updates the world timer set in the constructor to one that can be updated.
     * @param _info Callback info
     */
    @Inject(method = "<init>*", at = @At("RETURN"))
    protected void onConstructed(CallbackInfo _info) {
        this.renderTickCounter = FlashTimeState.INSTANCE.getWorldTimer().getTimer();
    }

    /**
     * Mixin that runs after the render() function and handles ticking the player's timer.
     * @param tick Whether or not this render is also a tick
     * @param _info Callback info
     */
    @Inject(method = "render", at = @At("RETURN"))
    protected void tickPlayerOnRender(boolean tick, CallbackInfo _info) {
        this.playerTickCounter.beginRenderTick(Util.getMeasuringTimeMs());
        FlashTimeState.INSTANCE.setLastTickDelta(this.playerTickCounter.tickDelta);
        if (tick) {
            IntStream.range(0, Math.min(10, this.playerTickCounter.ticksThisFrame))
                    .forEach(i -> {
                        FlashTimeState.INSTANCE.setUnlockPlayerTick(true);
                        this.playerTick();
                        FlashTimeState.INSTANCE.setUnlockPlayerTick(false);

                        if (FlashTimeState.INSTANCE.getSuperHot() && this.player != null && this.player.input != null) {
                            Input input = this.player.input;
                            if (input.pressingForward || input.pressingBack || input.pressingLeft || input.pressingRight) {
                                FlashTimeState.INSTANCE.getWorldTimer().setTps(20);
                            } else {
                                FlashTimeState.INSTANCE.getWorldTimer().setTps(0.5F);
                            }
                        }
                    });
        }
    }

    /**
     * The player parts of the tick function, split out from the regular tick function.
     */
    public void playerTick() {
        if (this.itemUseCooldown > 0) {
            --this.itemUseCooldown;
        }

        // MIXIN: `this.profiler` -> `this.getProfiler()`
        this.getProfiler().push("gui");
        if (!this.paused) {
            this.inGameHud.tick();
        }

        this.getProfiler().pop();
        this.gameRenderer.updateTargetedEntity(1.0F);
        // MIXIN: `this.tutorialManager` -> `this.getTutorialManager()`
        this.getTutorialManager().tick(this.world, this.crosshairTarget);
        this.getProfiler().push("gameMode");
        if (!this.paused && this.world != null) {
            this.interactionManager.tick();
        }

        this.getProfiler().swap("textures");
        if (this.world != null) {
            // MIXIN: `this.textureManager` -> `this.getTextureManager()`
            this.getTextureManager().tick();
        }

        if (this.currentScreen == null && this.player != null) {
            if (this.player.getHealth() <= 0.0F && !(this.currentScreen instanceof DeathScreen)) {
                this.openScreen((Screen) null);
            } else if (this.player.isSleeping() && this.world != null) {
                this.openScreen(new SleepingChatScreen());
            }
        } else if (this.currentScreen != null && this.currentScreen instanceof SleepingChatScreen && !this.player.isSleeping()) {
            this.openScreen((Screen) null);
        }

        if (this.currentScreen != null) {
            this.attackCooldown = 10000;
        }

        if (this.currentScreen != null) {
            Screen.wrapScreenError(() -> {
                this.currentScreen.tick();
            }, "Ticking screen", this.currentScreen.getClass().getCanonicalName());
        }

        if (!this.options.debugEnabled) {
            this.inGameHud.resetDebugHudChunk();
        }

        // MIXIN: `this.overlay` -> `this.getOverlay()`
        if (this.getOverlay() == null && (this.currentScreen == null || this.currentScreen.passEvents)) {
            this.getProfiler().swap("Keybindings");
            this.handleInputEvents();
            if (this.attackCooldown > 0) {
                --this.attackCooldown;
            }
        }

        this.getProfiler().swap("gameRenderer");

        if (this.world != null) {
            // MIXIN: Steal this from `tick`
            if (!this.paused) {
                this.gameRenderer.tick();
            }
            // MIXIN: Tick players separately
            world.tickEntity(this.player);
        }
    }

    /**
     * The world's side of the tick function.
     *
     * @author jD91mZM2
     * This is an overwrite mainly because the function doesn't need to be modified so much as it needs to be
     * split in two. There also needs to be *some* special care to make sure everything is in the right half
     * of the function.
     */
    @Overwrite
    public void tick() {
        if (this.world != null) {
            this.getProfiler().swap("levelRenderer");
            if (!this.paused) {
                this.worldRenderer.tick();
            }

            this.getProfiler().swap("level");
            if (!this.paused) {
                if (this.world.getLightningTicksLeft() > 0) {
                    this.world.setLightningTicksLeft(this.world.getLightningTicksLeft() - 1);
                }

                this.world.tickEntities();
            }
        } else if (this.gameRenderer.getShader() != null) {
            this.gameRenderer.disableShader();
        }

        if (!this.paused) {
            this.musicTracker.tick();
        }

        this.soundManager.tick(this.paused);
        if (this.world != null) {
            if (!this.paused) {
                this.world.setMobSpawnOptions(this.world.getDifficulty() != Difficulty.PEACEFUL, true);
                this.getTutorialManager().tick();

                try {
                    this.world.tick(() -> {
                        return true;
                    });
                } catch (Throwable var4) {
                    CrashReport crashReport = CrashReport.create(var4, "Exception in world tick");
                    if (this.world == null) {
                        CrashReportSection crashReportSection = crashReport.addElement("Affected level");
                        crashReportSection.add("Problem", (Object) "Level is null!");
                    } else {
                        this.world.addDetailsToCrashReport(crashReport);
                    }

                    throw new CrashException(crashReport);
                }
            }

            this.getProfiler().swap("animateTick");
            if (!this.paused && this.world != null) {
                this.world.doRandomBlockDisplayTicks(MathHelper.floor(this.player.getX()), MathHelper.floor(this.player.getY()), MathHelper.floor(this.player.getZ()));
            }

            this.getProfiler().swap("particles");
            if (!this.paused) {
                this.particleManager.tick();
            }
        } else if (this.clientConnection != null) {
            this.getProfiler().swap("pendingConnection");
            this.clientConnection.tick();
        }

        this.getProfiler().swap("keyboard");
        this.keyboard.pollDebugCrash();
        this.getProfiler().pop();
    }
}
