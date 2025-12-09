//import net.justlime.limeframegui.models.GuiItem
//import net.justlime.limeframegui.type.ChestGUI
//import org.bukkit.Material
//import org.junit.jupiter.api.AfterEach
//import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.jupiter.api.Assertions.assertTrue
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//import org.mockbukkit.mockbukkit.MockBukkit
//import org.mockbukkit.mockbukkit.ServerMock
//import org.mockbukkit.mockbukkit.simulate.entity.PlayerSimulation
//
//class LimeFrameTest {
//
//    private lateinit var server: ServerMock
//
//    @BeforeEach
//    fun setUp() {
//        server = MockBukkit.mock()
//        // Load your plugin to initialize LimeFrameAPI
//        MockBukkit.load(net.justlime.limeframegui.LimeFrameGUI::class.java)
//    }
//
//    @AfterEach
//    fun tearDown() {
//        MockBukkit.unmock()
//    }
//
//    @Test
//    fun `test multiplayer placeholders are isolated`() {
//        // 1. Create two players
//        val playerA = server.addPlayer("PlayerA")
//        val playerB = server.addPlayer("PlayerB")
//
//        // 2. Create the Blueprint
//        // Note: The item is created ONCE, but rendered differently for each player
//        val menu = ChestGUI(6, "Menu") {
//            val item = GuiItem(Material.DIAMOND, name = "Item for %player_name%")
//            setItem(item, 4)
//        }
//
//        // 3. Open for Player A
//        menu.open(playerA)
//        val viewA = playerA.openInventory
//        val itemA = viewA.topInventory.getItem(4)
//
//        // CHECK: Player A sees their own name
//        assertEquals("Item for PlayerA", itemA?.itemMeta?.displayName)
//
//        // 4. Open for Player B
//        menu.open(playerB)
//        val viewB = playerB.openInventory
//        val itemB = viewB.topInventory.getItem(4)
//
//        // CHECK: Player B sees their own name
//        assertEquals("Item for PlayerB", itemB?.itemMeta?.displayName)
//
//        // 5. ISOLATION CHECK: Ensure Player A's view didn't change to "PlayerB"
//        // This confirms your Session Architecture is working perfectly.
//        assertEquals("Item for PlayerA", itemA?.itemMeta?.displayName)
//    }
//
//    @Test
//    fun `test click events work`() {
//        val player = server.addPlayer("Tester")
//        var isClicked = false
//
//        val menu = ChestGUI(3, "Click Test") {
//            val item = GuiItem(Material.STONE)
//            setItem(item, 0) {
//                isClicked = true
//            }
//        }
//
//        menu.open(player)
//
//        // Using your correct syntax for simulation
//        // (Assuming PlayerSimulation is available or using player directly if it's PlayerMock)
//        PlayerSimulation(player).simulateInventoryClick(0)
//
//        assertTrue(isClicked, "Click listener failed to fire!")
//    }
//}