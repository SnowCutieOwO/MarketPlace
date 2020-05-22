package fr.fabienhebuterne.marketplace.services.inventory

import fr.fabienhebuterne.marketplace.commands.CommandListings
import fr.fabienhebuterne.marketplace.domain.base.Pagination
import fr.fabienhebuterne.marketplace.domain.paginated.Listings
import fr.fabienhebuterne.marketplace.domain.paginated.Paginated
import fr.fabienhebuterne.marketplace.services.pagination.ListingsService
import fr.fabienhebuterne.marketplace.storage.ListingsRepository
import fr.fabienhebuterne.marketplace.utils.formatInterval
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.text.MessageFormat
import java.util.*

class ListingsInventoryService(val listingsService: ListingsService, private val listingsRepository: ListingsRepository) : InventoryTypeService<Listings>(listingsService) {
    private val playersConfirmation: MutableMap<UUID, Paginated> = mutableMapOf()

    override fun initInventory(instance: JavaPlugin, pagination: Pagination<Listings>, player: Player): Inventory {
        val inventory = instance.server.createInventory(player, CommandListings.BIG_CHEST_SIZE, "MarketPlace - Achat")

        pagination.results.forEachIndexed { index, listings ->
            val itemStack = setBottomLore(listings.itemStack.clone(), listings)
            inventory.setItem(index, itemStack)
        }

        setBottomInventoryLine(inventory, pagination)

        return inventory
    }

    // TODO : Add step lore to confirm
    override fun setBottomLore(itemStack: ItemStack, paginated: Listings): ItemStack {
        val itemMeta = itemStack.itemMeta
        val loreItem = mutableListOf<String>()
        loreItem.add("")
        loreItem.add(MessageFormat.format("§6Seller: §e{0}", paginated.sellerPseudo))
        loreItem.add(MessageFormat.format("§6Price per item: §e{0}", paginated.price))
        loreItem.add(MessageFormat.format("§6Total available: §e{0} items", paginated.quantity))
        loreItem.add("")
        loreItem.add("§6► Left click to buy 1 item")

        if (paginated.quantity >= 2) {
            loreItem.add("§6► Middle click to buy custom quantity items")
        }

        if (paginated.quantity >= 64) {
            loreItem.add("§6► Right click to buy 64 items")
        }

        loreItem.add("")
        paginated.auditData.expiredAt?.let {
            loreItem.add("§6Expiration in " + formatInterval(it))
            loreItem.add("")
        }

        itemMeta.lore = loreItem
        itemStack.itemMeta = itemMeta
        return itemStack
    }

    fun confirmationAddNewItem(player: Player, listings: Listings): Inventory {
        playersConfirmation[player.uniqueId] = listings
        val inventory = Bukkit.createInventory(player, 9, "MarketPlace - Vente - Confirmation")

        val validItem = ItemStack(Material.STAINED_GLASS_PANE, 1, 5)
        val validItemMeta = validItem.itemMeta
        validItemMeta.displayName = "§aValider"
        validItem.itemMeta = validItemMeta
        inventory.setItem(2, validItem)

        val cancelItem = ItemStack(Material.STAINED_GLASS_PANE, 1, 14)
        val cancelItemMeta = cancelItem.itemMeta
        cancelItemMeta.displayName = "§cAnnuler"
        cancelItem.itemMeta = cancelItemMeta
        inventory.setItem(6, cancelItem)
        return inventory
    }

    fun clickOnAddNewItemConfirmation(event: InventoryClickEvent, player: Player) {
        if (event.slotType != InventoryType.SlotType.CONTAINER) {
            return
        }

        if (event.rawSlot == 2) {
            itemSell(player)
        }

        if (event.rawSlot == 6) {
            cancelSell(player)
        }
    }

    private fun itemSell(player: Player) {
        val paginated = playersConfirmation[player.uniqueId]
        if (paginated != null && paginated is Listings) {
            listingsService.create(player, paginated)
            playersConfirmation.remove(player.uniqueId)
        }
        player.closeInventory()
    }

    private fun cancelSell(player: Player) {
        val listings = playersConfirmation[player.uniqueId]
        if (listings != null) {
            playersConfirmation.remove(player.uniqueId)
        }
        player.sendMessage("cancelled sell item")
        player.closeInventory()
    }
}