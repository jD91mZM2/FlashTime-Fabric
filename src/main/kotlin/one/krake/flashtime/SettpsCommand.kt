package one.krake.flashtime

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.FloatArgumentType.floatArg
import com.mojang.brigadier.exceptions.CommandSyntaxException
import net.minecraft.client.network.packet.TitleS2CPacket
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.TranslatableText

enum class Target {
    Player,
    World,
}

object SettpsCommand {
    private val TICKSPEED = CommandManager.argument("ticks per second", floatArg(0.1f, 100.0f));

    /**
     * Registers the SettpsCommmand to the specified dispatcher.
     * @param dispatcher The dispatcher gotten from CommandRegistry.INSTANCE.register
     */
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("settickspeed")
                .then(
                    CommandManager.literal("player")
                        .then(TICKSPEED.executes() { ctx ->
                            executeSet(ctx.source, Target.Player, FloatArgumentType.getFloat(ctx, "ticks per second"))
                        })
                )
                .then(
                    CommandManager.literal("world")
                        .then(TICKSPEED.executes() { ctx ->
                            executeSet(ctx.source, Target.World, FloatArgumentType.getFloat(ctx, "ticks per second"))
                        })
                )
                .then(
                    CommandManager.literal("superhot")
                        .executes() { ctx ->
                            executeHot(ctx.source)
                        }
                )
        );
    }

    private fun executeSet(source: ServerCommandSource, target: Target, tps: Float): Int {
        FlashTimeState.superHot = false
        try {
            when (target) {
                Target.Player -> FlashTimeState.playerTimer.tps = tps
                Target.World -> FlashTimeState.worldTimer.tps = tps
            }
        } catch (err : Exception) {
            err.printStackTrace();
            throw err;
        }
        source.sendFeedback(TranslatableText("command.settickspeed.feedback"), true);
        return Command.SINGLE_SUCCESS;
    }

    private fun executeHot(source: ServerCommandSource): Int {
        FlashTimeState.superHot = true
        try {
            source.player.networkHandler.sendPacket(TitleS2CPacket(
                TitleS2CPacket.Action.TITLE,
                TranslatableText("command.settickspeed.superhot.title")
            ))
        } catch (_err : CommandSyntaxException) {
            // This is just for effect, I don't want the command to fail if source isn't a player
            source.sendFeedback(TranslatableText("command.settickspeed.superhot.feedback"), false)
        }
        return Command.SINGLE_SUCCESS;
    }
}