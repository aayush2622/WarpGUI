package mc.aayush262

import com.earth2me.essentials.Essentials
import com.earth2me.essentials.commands.WarpNotFoundException
import net.ess3.api.IUser
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.CompletableFuture

class WarpGUI : JavaPlugin() {

    lateinit var essentials: Essentials
        private set

    override fun onEnable() {
        val plugin = server.pluginManager.getPlugin("Essentials")
        if (plugin !is Essentials) {
            logger.severe("EssentialsX not found! Disabling WarpGUI.")
            server.pluginManager.disablePlugin(this)
            return
        }
        saveDefaultConfig()
        essentials = plugin
        logger.info("Hooked into EssentialsX!")

        getCommand("warpgui")?.apply {
            setExecutor(WarpCommand(this@WarpGUI))
            tabCompleter = WarpTabCompleter(essentials)
        }
    }

    override fun onDisable() {
        logger.info("WarpGUI disabled.")
    }
}

fun String.colorize(): Component =
    LegacyComponentSerializer.legacyAmpersand().deserialize(this)


class WarpCommand(private val plugin: WarpGUI) : CommandExecutor {
    override fun onCommand(
        sender: CommandSender, command: Command, label: String, args: Array<out String>
    ): Boolean {
        val prefix = plugin.config.getString("prefix")!!

        if (sender !is Player) {
            sender.sendMessage("$prefix Only players can use this command!".colorize())
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("$prefix Usage: /$label <warpName>".colorize())
            return true
        }

        val warpName = args[0]

        try {
            plugin.essentials.warps.getWarp(warpName)
            val user: IUser = plugin.essentials.getUser(sender)

            if (!user.isAuthorized("essentials.warp.$warpName")) {
                sender.sendMessage("$prefix You don't have permission to use this warp!".colorize())
                return true
            }

            user.asyncTeleport.warp(
                user,
                warpName,
                null,
                PlayerTeleportEvent.TeleportCause.PLUGIN,
                CompletableFuture()

            )
        } catch (_: WarpNotFoundException) {
            sender.sendMessage("$prefix §cWarp '$warpName' not found!".colorize())
        } catch (e: Exception) {
            sender.sendMessage("$prefix using warp: ${e.message}".colorize())
            e.printStackTrace()
        }
        return true
    }
}

class WarpTabCompleter(private val essentials: Essentials) : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> = if (args.size == 1) {
        essentials.warps.list
            .filter { it.startsWith(args[0], ignoreCase = true) }
            .sorted()
    } else {
        emptyList()
    }
}
