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
    private val mockPluginWithName = object : KotlinPlugin(JvmPluginDescription("org.test1.test1", "1.0.0", "test1")) {}
    private val mockPluginWithName2 =
        object : KotlinPlugin(JvmPluginDescription("org.test2.test2", "1.0.0", "test2")) {}

    @Test
    fun movingPluginPath() {
        PluginManager.pluginsDataPath.resolve(mockPlugin.name).mkdir()
        mockPlugin.load()
        assert(!MiraiConsole.job.isCancelled)
        PluginManager.pluginsDataPath.resolve(mockPluginWithName.name).mkdir()
        mockPluginWithName.load()
        assert(!MiraiConsole.job.isCancelled)
        PluginManager.pluginsDataPath.resolve(mockPluginWithName2.name).mkdir()
        PluginManager.pluginsDataPath.resolve(mockPluginWithName2.id).mkdir()
        mockPluginWithName2.load()
        assert(MiraiConsole.job.isCancelled)
        exceptCancel = false
    }
}