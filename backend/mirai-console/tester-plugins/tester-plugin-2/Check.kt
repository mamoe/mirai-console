/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 *  此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 *  Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 *  https://github.com/mamoe/mirai/blob/master/LICENSE
 */

@file:Suppress("PackageDirectoryMismatch")

package testsandbox2
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin

object Check {
    fun KotlinPlugin.check(v: Int) {
        if (v != 2) {
            error("Assertion failed.")
        }
        logger.info("Check ok")
    }
}
