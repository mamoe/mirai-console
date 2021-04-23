/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 *  此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 *  Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 *  https://github.com/mamoe/mirai/blob/master/LICENSE
 */

package net.mamoe.mirai.console.testing

import io.github.karlatemp.caller.CallerFinder
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import kotlin.system.exitProcess

open class BaseTestingPlugin(
    name: String = CallerFinder.getCaller().className.substringAfter('.')
) : KotlinPlugin(
    JvmPluginDescription(
        "net.mamoe.console.plugin.testing.$name", "1.0.0"
    )
) {
    protected open fun onEnable0() {}

    override fun onEnable() {
        try {
            onEnable0()
        } catch (e: Throwable) {
            logger.error(e)
            Thread.sleep(1000) // Wait error printed
            exitProcess(1)
        }
    }
}