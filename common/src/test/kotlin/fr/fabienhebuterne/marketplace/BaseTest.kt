package fr.fabienhebuterne.marketplace

import fr.fabienhebuterne.marketplace.domain.config.Config
import fr.fabienhebuterne.marketplace.domain.config.ConfigService
import fr.fabienhebuterne.marketplace.domain.config.Translation
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.serialization.UnsafeSerializationApi
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFactory
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.BeforeEach
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

abstract class BaseTest {

    private val resource: URL = this::class.java.classLoader.getResource("loader")
        ?: throw IllegalAccessException("ressource path not exist")
    private val filepath: Path = Paths.get(resource.toURI())
    val serverMock: Server = mockk()
    val javaPluginMock: JavaPlugin = mockk()
    val marketPlace: MarketPlace = mockk()
    var playerMock: Player = mockk()

    lateinit var translation: Translation
    lateinit var config: Config
    val fabienUuid: UUID = UUID.fromString("522841e6-a3b6-48dd-b67c-0b0f06ec1aa6")
    val ergailUuid: UUID = UUID.fromString("4a109300-ec09-4c47-9e8d-de735dd7f17f")

    @UnsafeSerializationApi
    @BeforeEach
    fun initDefault() {
        every { marketPlace.loader } returns javaPluginMock
        every { marketPlace.loader.dataFolder } returns filepath.toFile()
        every { marketPlace.loader.server } returns serverMock

        val configTranslationService = ConfigService(marketPlace, "translation-fr", Translation::class)
        configTranslationService.loadConfig()
        translation = configTranslationService.getSerialization()

        every { marketPlace.tl } returns configTranslationService.getSerialization()
        every { marketPlace.missingPermissionMessage } returns translation.errors.missingPermission
        every { marketPlace.reloadNotAvailableMessage } returns translation.errors.reloadNotAvailable

        val configService = ConfigService(marketPlace, "config", Config::class)
        configService.loadConfig()
        config = configService.getSerialization()

        every { marketPlace.conf } returns config

        // Mockk only for itemStack
        mockkStatic(Bukkit::class)
        val itemFactory: ItemFactory = mockk()
        every { Bukkit.getItemFactory() } returns itemFactory
        every { itemFactory.equals(null, null) } returns false
        every { itemFactory.getItemMeta(any()) } returns null
    }

    @BeforeEach
    fun initPlayerMock() {
        // Reset this mock on each test
        playerMock = mockk()
        every { playerMock.uniqueId } returns fabienUuid
        every { playerMock.name } returns "Fabien91"
        every { playerMock.world.name } returns "world"
    }

}
