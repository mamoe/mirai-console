/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 *  此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 *  Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 *  https://github.com/mamoe/mirai/blob/master/LICENSE
 */

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("java")
}

kotlin {
    sourceSets {
        all {
            languageSettings.optIn("kotlin.Experimental")
            languageSettings.optIn("kotlin.RequiresOptIn")
            languageSettings.progressiveMode = true
            languageSettings.optIn("net.mamoe.mirai.utils.MiraiInternalAPI")
            languageSettings.optIn("kotlin.ExperimentalUnsignedTypes")
            languageSettings.optIn("kotlin.experimental.ExperimentalTypeInference")
            languageSettings.optIn("kotlin.contracts.ExperimentalContracts")
        }
    }
}

dependencies {
    api(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    api(`mirai-core-utils`)
}