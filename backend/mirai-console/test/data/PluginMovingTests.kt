package net.mamoe.mirai.console.data

import net.mamoe.mirai.console.AbstractConsoleTest
import net.mamoe.mirai.console.MiraiConsole
import net.mamoe.mirai.console.internal.data.mkdir
import net.mamoe.mirai.console.plugin.PluginManager
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.load
import net.mamoe.mirai.console.plugin.id
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.plugin.name
import org.junit.jupiter.api.Test

class PluginMovingTests : AbstractConsoleTest() {
    private val mockPluginWithName = object : KotlinPlugin(JvmPluginDescription("org.test.test", "1.0.0", "test")) {}

    @Test
    fun `plugin with same name and id`() {
        PluginManager.pluginsDataPath.resolve(mockPlugin.name).mkdir()
        mockPlugin.load()
        assert(!MiraiConsole.job.isCancelled)
    }

    @Test
    fun normalMove() {
        PluginManager.pluginsDataPath.resolve(mockPluginWithName.name).mkdir()
        mockPluginWithName.load()
        assert(!MiraiConsole.job.isCancelled)
    }

    @Test
    fun `plugin id path occupied`() {
        PluginManager.pluginsDataPath.resolve(mockPluginWithName.name).mkdir()
        PluginManager.pluginsDataPath.resolve(mockPluginWithName.id).mkdir()
        mockPluginWithName.load()
        assert(MiraiConsole.job.isCancelled)
    }
}