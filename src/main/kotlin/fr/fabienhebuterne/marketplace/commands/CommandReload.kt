package fr.fabienhebuterne.marketplace.commands

import fr.fabienhebuterne.marketplace.MarketPlace
import fr.fabienhebuterne.marketplace.commands.factory.CallCommand
import fr.fabienhebuterne.marketplace.domain.reloadTranslation
import fr.fabienhebuterne.marketplace.tl
import kotlinx.serialization.ImplicitReflectionSerializer
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.entity.Player
import org.kodein.di.Kodein

class CommandReload(kodein: Kodein) : CallCommand<MarketPlace>("reload") {

    @ImplicitReflectionSerializer
    override fun runFromPlayer(server: Server, player: Player, commandLabel: String, cmd: Command, args: Array<String>) {
        // TODO : Put this in common code (callCommand)
        if (MarketPlace.isReload) {
            player.sendMessage(tl.errors.reloadNotAvailable)
            return
        }

        MarketPlace.isReload = true

        player.sendMessage(tl.commandReloadStart)
        Bukkit.getOnlinePlayers().forEach {
            if (it.openInventory.title.contains("MarketPlace")) {
                it.closeInventory()
            }
        }

        instance.config.loadConfig()
        instance.translation.loadConfig()
        tl = instance.translation.getSerialization()
        reloadTranslation()

        player.sendMessage(tl.commandReloadFinish)
        MarketPlace.isReload = false
    }

}