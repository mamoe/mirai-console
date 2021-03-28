/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 *  此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 *  Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 *  https://github.com/mamoe/mirai/blob/master/LICENSE
 */


package net.mamoe.mirai.console.intellij.creator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import net.mamoe.mirai.console.intellij.creator.MiraiVersionKind.Companion.getMiraiVersionListAsync
import net.mamoe.mirai.console.intellij.creator.steps.BuildSystemType
import net.mamoe.mirai.console.intellij.creator.steps.LanguageType
import net.mamoe.mirai.console.intellij.creator.tasks.adjustToClassName
import kotlin.contracts.contract

data class ProjectCoordinates(
    val groupId: String, // already checked by pattern
    val artifactId: String,
    val version: String
) {
    val packageName: String get() = groupId
}

data class PluginCoordinates(
    val id: String?,
    val name: String?,
    val author: String?,
    val info: String?,
    val dependsOn: String?,
)

class MiraiProjectModel private constructor() {
    // STEP: ProjectCreator

    var projectCoordinates: ProjectCoordinates? = null
    var buildSystemType: BuildSystemType = BuildSystemType.DEFAULT
    var languageType: LanguageType = LanguageType.DEFAULT

    var miraiVersion: String? = null
    var pluginCoordinates: PluginCoordinates? = null

    val mainClassName: String
        get() = pluginCoordinates?.run { name?.adjustToClassName() ?: id?.adjustToClassName() } ?: "PluginMain"

    val packageName: String get() = projectCoordinates.checkNotNull("projectCoordinates").groupId


    var availableMiraiVersions: Deferred<Set<MiraiVersion>>? = null
    val availableMiraiVersionsOrFail get() = availableMiraiVersions.checkNotNull("availableMiraiVersions")

    fun checkValuesNotNull() {
        checkNotNull(miraiVersion) { "miraiVersion" }
        checkNotNull(pluginCoordinates) { "pluginCoordinates" }
        checkNotNull(projectCoordinates) { "projectCoordinates" }
    }

    companion object {
        fun create(scope: CoroutineScope): MiraiProjectModel {
            return MiraiProjectModel().apply {
                availableMiraiVersions = scope.getMiraiVersionListAsync()
            }
        }
    }
}

fun <T : Any> T?.checkNotNull(name: String): T {
    contract {
        returns() implies (this@checkNotNull != null)
    }
    checkNotNull(this) {
        "$name is not yet initialized."
    }
    return this
}