package su.plo.voice.discs.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import su.plo.lib.api.chat.MinecraftTextComponent
import su.plo.lib.api.server.command.MinecraftCommandSource
import su.plo.lib.api.server.permission.PermissionDefault
import su.plo.voice.discs.DiscsPlugin
import su.plo.voice.discs.utils.extend.asPlayer
import su.plo.voice.discs.utils.extend.asVoicePlayer
import java.util.concurrent.ConcurrentHashMap

open class CommandHandler(
    val plugin: DiscsPlugin,
): CommandExecutor, TabCompleter {

    private val subCommands: MutableMap<String, SubCommand> = ConcurrentHashMap()

    fun <T : SubCommand> addSubCommand(subCommand: (handler: CommandHandler) -> T): CommandHandler {
        subCommand(this)
            .also { subCommands[it.name] = it }
            .also { registerPermissions(it.permissions) }
        return this
    }

    private fun registerPermissions(permissions: List<Pair<String, PermissionDefault>>) {
        permissions.forEach {
            plugin.voiceServer.minecraftServer.permissionsManager.register(
                "pv.addon.discs.${it.first}",
                it.second
            )
        }
    }

    private val unknownCommandComponent: MinecraftTextComponent
        get() = MinecraftTextComponent.translatable(
            "pv.addon.discs.error.unknown_subcommand",
            subCommands.keys.joinToString(", ")
        )

    override fun onCommand(sender: CommandSender, command: Command, label: String, arguments: Array<out String>): Boolean {

        val voicePlayer = sender.asPlayer()?.asVoicePlayer(plugin.voiceServer) ?: run {
            return false
        }

        val subCommand = arguments.getOrNull(0) ?: run {
            voicePlayer.instance.sendMessage(unknownCommandComponent)
            return false
        }

        subCommands[subCommand]?.let {
            it.execute(sender, arguments)
            return true
        }

        voicePlayer.instance.sendMessage(unknownCommandComponent)

        return false
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        arguments: Array<out String>,
    ): List<String> {

        if (arguments.isEmpty()) return subCommands
            .filter { it.value.checkCanExecute(sender) }
            .keys
            .toList()

        val subCommand = arguments.getOrNull(0) ?: return listOf()

        if (arguments.size == 1) return subCommands
            .filter { it.key.startsWith(subCommand) && it.value.checkCanExecute(sender) }
            .keys
            .toList()

        subCommands[subCommand]?.let { return it.suggest(sender, arguments) }

        return listOf()
    }

    fun getTranslationStringByKey(key: String, source: MinecraftCommandSource): String {
        return plugin.voiceServer.languages.getServerLanguage(source)[key] ?: key
    }
}
