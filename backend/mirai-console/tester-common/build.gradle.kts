/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

import java.time.Duration
import java.time.Instant

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

kotlin {
    explicitApiWarning()
}

dependencies {
    api(project(":mirai-console"))
    api(`mirai-core-api`)
    api(`mirai-core-utils`)
    api(`kotlin-stdlib-jdk8`)
    api(`kotlinx-coroutines-core`)
    api(`kotlin-reflect`)
    api(`jetbrains-annotations`)
    api(`caller-finder`)
    api(kotlin("test-junit5"))
    api("org.junit.jupiter:junit-jupiter-api:${Versions.junit}")
}

val testers = mutableListOf<Project>()
val currentProject = project

tasks.register("runTerminalDaemon", JavaExec::class.java) {

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

    doFirst {
        sandbox.mkdirs()
        val plugins = sandbox.resolve("plugins")
        plugins.deleteRecursively()
        plugins.mkdirs()
        testers.forEach { testerProject ->
            val jarTask = testerProject.tasks.getByName("jar")
            jarTask.outputs.files.forEach { it.copyTo(plugins.resolve(it.name), true) }
        }
    }
    timeout.set(Duration.ofMinutes(5))
}

tasks.getByName("check").dependsOn("runTerminalDaemon")

rootProject.allprojects {
    val proj = this@allprojects
    if (proj.name.removePrefix(":").startsWith("mirai-console-plugin-tester-")) {
        if (proj != currentProject) {
            testers.add(proj)
            currentProject.tasks.named("runTerminalDaemon") {
                dependsOn(":" + proj.name.removePrefix(":") + ":jar")
            }
        }
    }
}

