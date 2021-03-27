/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 *  此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 *  Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 *  https://github.com/mamoe/mirai/blob/master/LICENSE
 */


@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package net.mamoe.mirai.console.gradle

import net.mamoe.mirai.console.compiler.common.CheckerConstants
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.get
import java.io.File

public class PluginDescription(
    project: Project
) {
    public var id: String? = null
    public var name: String? = project.displayName
    public var info: String? = project.runHierarchically { description }
    public var author: String? = null
    public var version: String? = project.version.toString()
    public var dependencies: MutableList<String> = mutableListOf()

    public fun addDependency(pluginId: String, version: String) {
        dependencies.add("$pluginId:$version")
    }
}

/**
 * Generates `JvmPluginDescription` YAML file, e.g. `plugin.yml`.
 */
@CacheableTask
public class GeneratePluginDescriptionTask : DefaultTask() {

    @Input
    public val pluginDescription: PluginDescription = PluginDescription(project)

    @Input
    public var filename: String = "plugin.yml"

    public fun pluginDescription(
        action: Action<PluginDescription>
    ) {
        return action.execute(pluginDescription)
    }

    @OutputFile
    public fun getOutputFile(): File {
        val resources = project.sourceSets["main"].resources.sourceDirectories
        if (resources.isEmpty) {
            error("Source set 'main' does not have directory 'resources'.")
        }
        return resources.files.first().resolve(filename)
    }

    @TaskAction
    public fun generateFile() {
        val text = generateFileContent()
        getOutputFile().writeText(text)
    }

    private fun generateFileContent(): String = buildString {
        pluginDescription.run {
            val pluginId = id
            require(pluginId?.matches(CheckerConstants.PLUGIN_ID_REGEX) == true) { "Invalid pluginId: '$pluginId'" }
            val pluginName = name
            require(pluginName == null || CheckerConstants.PLUGIN_FORBIDDEN_NAMES.none { it == pluginName }) { "Invalid pluginName: '$pluginName'" }

            appendLine("id: $pluginId")
            pluginName?.let { appendLine("name: $it") }
            version?.let { appendLine("version: $it") }
            author?.let { appendLine("author: $it") }
            info?.let { appendLine("info: $it") }

            val dependencies = dependencies
            if (dependencies.isNotEmpty()) {
                appendLine("dependencies: ")
                for (dependency in dependencies) {
                    appendLine("  - '$dependency'")
                }
            }
        }
    }
}

private fun StringBuilder.appendLine(text: String = ""): StringBuilder = append(text).append("\n")
