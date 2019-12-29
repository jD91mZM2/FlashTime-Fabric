package one.krake.flashtime.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SleepingChatScreen;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.stream.IntStream;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    private final RenderTickCounter renderTickCounter = new RenderTickCounter(2.0F, 0L);
    private final RenderTickCounter playerTickCounter = new RenderTickCounter(20.0F, 0L);

    @Shadow
    public GameRenderer gameRenderer;
    @Shadow
    private boolean paused;
    @Shadow
    public ClientWorld world;
    @Shadow
    private int itemUseCooldown;
    @Shadow
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
    protected int attackCooldown;
    @Shadow
    public GameOptions options;

    @Shadow
    public Profiler getProfiler() { return null; }
    @Shadow
    public TutorialManager getTutorialManager() { return null; }
    @Shadow
    public TextureManager getTextureManager() { return null; }
    @Shadow
    public Overlay getOverlay() { return null; }
    @Shadow
    public void openScreen(Screen screen) {}
    @Shadow
    private void handleInputEvents() {}
    @Shadow
    public WorldRenderer worldRenderer;
    @Shadow
    private MusicTracker musicTracker;
    @Shadow
    private SoundManager soundManager;
    @Shadow
    public ParticleManager particleManager;
    @Shadow
    private ClientConnection clientConnection;
    @Shadow
    public Keyboard keyboard;

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(boolean tick, CallbackInfo _info) {
        if (tick) {
            this.playerTickCounter.beginRenderTick(Util.getMeasuringTimeMs());
            IntStream.range(0, Math.min(10, this.playerTickCounter.ticksThisFrame))
                    .forEach(i -> {
                        this.playerTick();
                    });
        }
    }

    // Split out from tick function
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
            if (!this.paused) {
                this.gameRenderer.tick();
            }
            world.tickEntity(this.player);
        }
    }

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
                        crashReportSection.add("Problem", (Object)"Level is null!");
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