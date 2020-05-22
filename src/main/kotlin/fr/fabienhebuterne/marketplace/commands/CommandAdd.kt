package fr.fabienhebuterne.marketplace.commands

import fr.fabienhebuterne.marketplace.MarketPlace
import fr.fabienhebuterne.marketplace.commands.factory.CallCommand
import fr.fabienhebuterne.marketplace.domain.base.AuditData
import fr.fabienhebuterne.marketplace.domain.paginated.Listings
import fr.fabienhebuterne.marketplace.exceptions.BadArgumentException
import fr.fabienhebuterne.marketplace.exceptions.HandEmptyException
import fr.fabienhebuterne.marketplace.services.inventory.ListingsInventoryService
import fr.fabienhebuterne.marketplace.services.pagination.ListingsService
import fr.fabienhebuterne.marketplace.storage.ListingsRepository
import fr.fabienhebuterne.marketplace.utils.longIsValid
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.kodein.di.Kodein
import org.kodein.di.generic.instance
import java.util.*

class CommandAdd(kodein: Kodein) : CallCommand<MarketPlace>("add") {

    private val listingsRepository: ListingsRepository by kodein.instance<ListingsRepository>()
    private val listingsService: ListingsService by kodein.instance<ListingsService>()
    private val listingsInventoryService: ListingsInventoryService by kodein.instance<ListingsInventoryService>()

    override fun runFromPlayer(server: Server, player: Player, commandLabel: String, cmd: Command, args: Array<String>) {
        if (player.itemInHand.type == Material.AIR) {
            throw HandEmptyException(player)
        }

        if (args.size <= 1) {
            throw BadArgumentException(player, "§cUsage: /marketplace add <money>")
        }

        if (!longIsValid(args[1])) {
            throw BadArgumentException(player, "${args[1]} is not a valid number")
        }

        val money = args[1].toLong()
        val currentItemStack = player.itemInHand

        val currentItemStackOne = currentItemStack.clone()
        currentItemStackOne.amount = 1

        val listings = Listings(
                sellerUuid = player.uniqueId,
                sellerPseudo = player.name,
                itemStack = currentItemStackOne,
                quantity = currentItemStack.amount,
                price = money,
                world = player.world.name,
                auditData = AuditData(
                        System.currentTimeMillis(),
                        System.currentTimeMillis(),
                        System.currentTimeMillis() + (3600 * 24 * 7 * 1000)
                )
        )

        val findExistingListings = listingsRepository.find(listings.sellerUuid, listings.itemStack, listings.price)

        if (findExistingListings != null) {
            listingsService.updateListings(findExistingListings, currentItemStack, player)
        } else {
            val confirmationAddNewItemInventory = listingsInventoryService.confirmationAddNewItem(player, listings)
            player.openInventory(confirmationAddNewItemInventory)
        }
    }




}