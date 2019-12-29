package one.krake.flashtime

import net.fabricmc.api.ModInitializer

object FlashTimeState {
    var tickDelayMillis = 1000 / 2;
}

class FlashTime: ModInitializer {
    override fun onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        println("Hello Fabric world!")
    }
}

