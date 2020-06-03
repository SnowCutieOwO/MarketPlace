package fr.fabienhebuterne.marketplace.storage.mysql

import fr.fabienhebuterne.marketplace.domain.base.AuditData
import fr.fabienhebuterne.marketplace.domain.base.Filter
import fr.fabienhebuterne.marketplace.domain.base.FilterName
import fr.fabienhebuterne.marketplace.domain.base.FilterType
import fr.fabienhebuterne.marketplace.domain.paginated.Listings
import fr.fabienhebuterne.marketplace.json.ITEMSTACK_MODULE
import fr.fabienhebuterne.marketplace.json.ItemStackSerializer
import fr.fabienhebuterne.marketplace.storage.ListingsRepository
import fr.fabienhebuterne.marketplace.storage.mysql.ListingsTable.createdAt
import fr.fabienhebuterne.marketplace.storage.mysql.ListingsTable.expiredAt
import fr.fabienhebuterne.marketplace.storage.mysql.ListingsTable.id
import fr.fabienhebuterne.marketplace.storage.mysql.ListingsTable.itemStack
import fr.fabienhebuterne.marketplace.storage.mysql.ListingsTable.price
import fr.fabienhebuterne.marketplace.storage.mysql.ListingsTable.quantity
import fr.fabienhebuterne.marketplace.storage.mysql.ListingsTable.sellerPseudo
import fr.fabienhebuterne.marketplace.storage.mysql.ListingsTable.sellerUuid
import fr.fabienhebuterne.marketplace.storage.mysql.ListingsTable.updatedAt
import fr.fabienhebuterne.marketplace.storage.mysql.ListingsTable.world
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object ListingsTable : UUIDTable("marketplace_listings") {
    val sellerUuid = ListingsTable.varchar("seller_uuid", 36)
    val sellerPseudo = ListingsTable.varchar("seller_pseudo", 16)
    val itemStack = ListingsTable.text("item_stack")
    val quantity = ListingsTable.integer("quantity")
    val price = ListingsTable.long("price")
    val world = ListingsTable.varchar("world", 200)
    val createdAt = ListingsTable.long("created_at")
    val updatedAt = ListingsTable.long("updated_at")
    val expiredAt = ListingsTable.long("expired_at")
}

class ListingsRepositoryImpl(private val marketPlaceDb: Database) : ListingsRepository {
    private val json = Json(JsonConfiguration.Stable, context = ITEMSTACK_MODULE)

    override fun fromRow(row: ResultRow): Listings {
        val itemStack: ItemStack = json.parse(ItemStackSerializer, row[itemStack])

        return Listings(
                id = row[id].value,
                sellerUuid = UUID.fromString(row[sellerUuid]),
                sellerPseudo = row[sellerPseudo],
                itemStack = itemStack,
                quantity = row[quantity],
                price = row[price],
                world = row[world],
                auditData = AuditData(
                        createdAt = row[createdAt],
                        updatedAt = row[updatedAt],
                        expiredAt = row[expiredAt]
                )
        )
    }

    override fun fromEntity(insertTo: UpdateBuilder<Number>, entity: Listings): UpdateBuilder<Number> {
        val itemStackString = json.stringify(ItemStackSerializer, entity.itemStack)

        entity.id?.let { insertTo[id] = EntityID(it, ListingsTable) }
        insertTo[sellerUuid] = entity.sellerUuid.toString()
        insertTo[sellerPseudo] = entity.sellerPseudo
        insertTo[itemStack] = itemStackString
        insertTo[quantity] = entity.quantity
        insertTo[price] = entity.price
        insertTo[world] = entity.world
        insertTo[createdAt] = entity.auditData.createdAt
        if (entity.auditData.updatedAt != null) {
            insertTo[updatedAt] = entity.auditData.updatedAt
        }
        if (entity.auditData.expiredAt != null) {
            insertTo[expiredAt] = entity.auditData.expiredAt
        }
        return insertTo
    }

    private fun filterDomainToStorage(filter: Filter): Pair<Column<*>, SortOrder> {
        val filterNameConverted = when (filter.filterName) {
            FilterName.CREATED_AT -> createdAt
            FilterName.EXPIRED_AT -> expiredAt
            FilterName.PRICE -> price
        }

        val filterTypeConverted = when (filter.filterType) {
            FilterType.ASC -> SortOrder.ASC
            FilterType.DESC -> SortOrder.DESC
        }

        return Pair(filterNameConverted, filterTypeConverted)
    }

    override fun findAll(uuid: UUID?, from: Int?, to: Int?, searchKeyword: String?, filter: Filter): List<Listings> {
        return transaction(marketPlaceDb) {
            // TODO : Find better solution refactoring with extraction ?
            val selectBase = if (uuid == null) {
                ListingsTable.selectAll()
            } else {
                if (searchKeyword == null) {
                    ListingsTable.select {
                        sellerUuid eq uuid.toString()
                    }
                } else {
                    ListingsTable.select {
                        sellerUuid eq uuid.toString() and (itemStack like "%$searchKeyword%")
                    }
                }
            }

            when {
                from != null && to != null -> {
                    selectBase
                            .limit(to, from.toLong())
                            .orderBy(filterDomainToStorage(filter))
                            .map { fromRow(it) }
                }
                from == null && to == null -> {
                    selectBase
                            .orderBy(filterDomainToStorage(filter))
                            .map { fromRow(it) }
                }
                else -> {
                    selectBase
                            .orderBy(filterDomainToStorage(filter))
                            .map { fromRow(it) }
                }
            }
        }
    }

    override fun find(id: String): Listings? {
        TODO("Not yet implemented")
    }

    override fun find(sellerUuid: UUID, itemStack: ItemStack, price: Long): Listings? {
        val itemStackString = json.stringify(ItemStackSerializer, itemStack)

        return transaction(marketPlaceDb) {
            ListingsTable.select {
                (ListingsTable.sellerUuid eq sellerUuid.toString()) and
                        (ListingsTable.itemStack eq itemStackString) and
                        (ListingsTable.price eq price)
            }.map { fromRow(it) }.firstOrNull()
        }
    }

    override fun findByUUID(sellerUuid: UUID): List<Listings> {
        return transaction(marketPlaceDb) {
            ListingsTable.select {
                ListingsTable.sellerUuid eq sellerUuid.toString()
            }.map { fromRow(it) }
        }
    }

    override fun create(entity: Listings): Listings {
        transaction(marketPlaceDb) {
            ListingsTable.insert { fromEntity(it, entity) }
        }
        return entity
    }

    override fun update(entity: Listings): Listings {
        val itemStackString = json.stringify(ItemStackSerializer, entity.itemStack)

        transaction(marketPlaceDb) {
            ListingsTable.update({
                (sellerUuid eq entity.sellerUuid.toString()) and
                        (itemStack eq itemStackString) and
                        (price eq entity.price)
            }) {
                fromEntity(it, entity)
            }
        }
        return entity
    }

    override fun delete(id: UUID) {
        transaction(marketPlaceDb) {
            ListingsTable.deleteWhere { ListingsTable.id eq id }
        }
    }

    override fun countAll(uuid: UUID?, searchKeyword: String?): Int {
        return transaction(marketPlaceDb) {
            when (searchKeyword == null) {
                true -> ListingsTable.selectAll().count().toInt()
                false -> ListingsTable.select { itemStack like "%$searchKeyword%" }.count().toInt()
            }
        }
    }
}