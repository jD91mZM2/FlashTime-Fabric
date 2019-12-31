package one.krake.flashtime

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.FloatArgumentType.floatArg
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource

enum class Target {
    Player,
    World,
}

object SettpsCommand {
    private val TICKSPEED = CommandManager.argument("ticks per second", floatArg(0.1f, 100.0f));

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("settickspeed")
                .then(
                    CommandManager.literal("player")
                        .then(TICKSPEED.executes() { ctx ->
                            execute(Target.Player, FloatArgumentType.getFloat(ctx, "ticks per second"))
                        })
                )
                .then(
                    CommandManager.literal("world")
                        .then(TICKSPEED.executes() { ctx ->
                            execute(Target.World, FloatArgumentType.getFloat(ctx, "ticks per second"))
                        })
                )
        );
    }

    private fun execute(target: Target, tps: Float): Int {
        try {
            when (target) {
                Target.Player -> FlashTimeState.playerTimer.tps = tps
                Target.World -> FlashTimeState.worldTimer.tps = tps
            }
        } catch (err : Exception) {
            err.printStackTrace();
            throw err;
        }
        return 1;
    }
}