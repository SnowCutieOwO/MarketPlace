package fr.fabienhebuterne.marketplace.storage.mysql

import fr.fabienhebuterne.marketplace.domain.Mails
import fr.fabienhebuterne.marketplace.domain.base.AuditData
import fr.fabienhebuterne.marketplace.json.ITEMSTACK_MODULE
import fr.fabienhebuterne.marketplace.json.ItemStackSerializer
import fr.fabienhebuterne.marketplace.storage.MailsRepository
import fr.fabienhebuterne.marketplace.storage.mysql.ListingsTable.createdAt
import fr.fabienhebuterne.marketplace.storage.mysql.ListingsTable.expiredAt
import fr.fabienhebuterne.marketplace.storage.mysql.ListingsTable.id
import fr.fabienhebuterne.marketplace.storage.mysql.ListingsTable.itemStack
import fr.fabienhebuterne.marketplace.storage.mysql.ListingsTable.quantity
import fr.fabienhebuterne.marketplace.storage.mysql.ListingsTable.updatedAt
import fr.fabienhebuterne.marketplace.storage.mysql.MailsTable.playerUuid
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object MailsTable : UUIDTable("marketplace_mails") {
    val playerUuid = MailsTable.varchar("player_uuid", 36)
    val itemStack = MailsTable.text("item_stack")
    val quantity = MailsTable.integer("quantity")
    val createdAt = MailsTable.long("created_at")
    val updatedAt = MailsTable.long("updated_at")
    val expiredAt = MailsTable.long("expired_at")
}

class MailsRepositoryImpl(private val marketPlaceDb: Database) : MailsRepository {
    private val json = Json(JsonConfiguration.Stable, context = ITEMSTACK_MODULE)

    override fun fromRow(row: ResultRow): Mails {
        val itemStack: ItemStack = json.parse(ItemStackSerializer, row[itemStack])

        return Mails(
                id = row[MailsTable.id].value,
                playerUuid = row[playerUuid],
                itemStack = itemStack,
                quantity = row[quantity],
                auditData = AuditData(
                        createdAt = row[createdAt],
                        updatedAt = row[updatedAt],
                        expiredAt = row[expiredAt]
                )
        )
    }

    override fun fromEntity(insertTo: UpdateBuilder<Number>, entity: Mails): UpdateBuilder<Number> {
        val itemStackString = json.stringify(ItemStackSerializer, entity.itemStack)

        insertTo[id] = EntityID(entity.id, MailsTable)
        insertTo[playerUuid] = entity.playerUuid
        insertTo[itemStack] = itemStackString
        insertTo[quantity] = entity.quantity
        insertTo[createdAt] = entity.auditData.createdAt
        insertTo[updatedAt] = entity.auditData.updatedAt
        insertTo[expiredAt] = entity.auditData.expiredAt
        return insertTo
    }

    override fun findAll(from: Int?, to: Int?): List<Mails> {
        TODO("Not yet implemented")
    }

    override fun find(id: String): Mails? {
        TODO("Not yet implemented")
    }

    override fun findByUUID(playerUuid: UUID): List<Mails> {
        return transaction(marketPlaceDb) {
            MailsTable.select {
                MailsTable.playerUuid eq playerUuid.toString()
            }.map { fromRow(it) }
        }
    }

    override fun create(entity: Mails): Mails {
        transaction(marketPlaceDb) {
            MailsTable.insert { fromEntity(it, entity) }
        }
        return entity
    }

    override fun update(entity: Mails): Mails {
        val itemStackString = json.stringify(ItemStackSerializer, entity.itemStack)

        transaction(marketPlaceDb) {
            MailsTable.update({
                (playerUuid eq entity.playerUuid) and
                        (itemStack eq itemStackString)
            }) {
                fromEntity(it, entity)
            }
        }
        return entity
    }

    override fun delete(id: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun countAll(): Int {
        return transaction(marketPlaceDb) {
            MailsTable.selectAll().count().toInt()
        }
    }
}