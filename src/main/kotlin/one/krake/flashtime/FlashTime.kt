package one.krake.flashtime

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.registry.CommandRegistry
import net.minecraft.client.render.RenderTickCounter

class TickSpeed constructor(
    tps: Float
) {
    private var _tps = tps

    var tps : Float
        get() = this._tps
        set(newTps) {
            this._tps = newTps;
            (this.timer as IRenderTickCounterAdditions).setTps(newTps)
        }

    var timer = RenderTickCounter(tps, 0L)
}

object FlashTimeState {
    var worldTimer = TickSpeed(2.0f)
    var playerTimer = TickSpeed(20.0f)
    var superHot = false
    var lastTickDelta = 0.0
    var unlockPlayerTick = false
}

class FlashTime : ModInitializer {
    override fun onInitialize() {
        CommandRegistry.INSTANCE.register(false) { dispatcher ->
            SettpsCommand.register(dispatcher)
        }
    }
}

