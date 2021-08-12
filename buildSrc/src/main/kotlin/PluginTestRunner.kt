/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

import org.gradle.api.Project
import org.gradle.kotlin.dsl.get
import java.time.Duration

/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

object PluginTestRunner {
    fun registerPluginTestRunnerTask(
        project: Project,
        vararg pluginProjectPaths: String
    ) = project.registerPluginTestRunnerTask0(*pluginProjectPaths)

    private fun Project.registerPluginTestRunnerTask0(
        vararg pluginProjectPaths: String
    ) {
        val pluginProjects = pluginProjectPaths.map { project(it) }

        val currentProject = project

        tasks.register("runTerminalDaemon") {
            for (plugin in pluginProjects) {
                dependsOn(plugin.tasks.getByName("jar"))
            }
            group = "verification"

            timeout.set(Duration.ofMinutes(5))
            doFirst {
                javaexec {
                    val terminal = project(":mirai-console-terminal")
                    val console = project(":mirai-console")
                    classpath = terminal.configurations["testRuntimeClasspath"]
                    classpath += console.configurations["testRuntimeClasspath"]
                    classpath += console.sourceSets.getByName("main").output // console
                    classpath += console.sourceSets.getByName("test").output // console
                    classpath += terminal.sourceSets.getByName("main").output
                    classpath += currentProject.tasks.getByName("jar").outputs.files
                    mainClass.set("net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader")

                    val sandbox = project.buildDir.resolve("runTerminalDaemon")
                    this.workingDir = sandbox


                    sandbox.mkdirs()
                    val plugins = sandbox.resolve("plugins")
                    plugins.deleteRecursively()
                    plugins.mkdirs()
                    pluginProjects.forEach { testerProject ->
                        val jarTask = testerProject.tasks.getByName("jar")
                        jarTask.outputs.files.forEach { it.copyTo(plugins.resolve(it.name), true) }
                    }
                }
            }
        }

        tasks.getByName("check").dependsOn("runTerminalDaemon")
    }
}