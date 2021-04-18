/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */
@file:Suppress("MemberVisibilityCanBePrivate")

package net.mamoe.mirai.console.internal.plugin

import net.mamoe.mirai.console.plugin.jvm.ExportManager
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal class JvmPluginClassLoader(
    val file: File,
    parent: ClassLoader?,
    val classLoaders: Collection<JvmPluginClassLoader>,
) : URLClassLoader(arrayOf(file.toURI().toURL()), parent) {
    //// 只允许插件 getResource 时获取插件自身资源, #205
    override fun getResources(name: String?): Enumeration<URL> = findResources(name)
    override fun getResource(name: String?): URL? = findResource(name)
    // getResourceAsStream 在 URLClassLoader 中通过 getResource 确定资源
    //      因此无需 override getResourceAsStream

    override fun toString(): String {
        return "JvmPluginClassLoader{source=$file}"
    }

    internal var declaredFilter: ExportManager? = null

    companion object {
        val loadingLock = ConcurrentHashMap<String, Any>()

        init {
            ClassLoader.registerAsParallelCapable()
        }
    }

    internal fun loadClassFromOtherClassLoader(name: String): Class<*>? {
        findLoadedClass(name)?.let { return it }
        return try {
            super.findClass(name)
        } catch (e: ClassNotFoundException) {
            null
        }
    }

    internal fun loadedClass(name: String): Class<*>? = super.findLoadedClass(name)

    override fun findClass(name: String): Class<*> {
        synchronized(kotlin.run {
            val lock = Any()
            loadingLock.putIfAbsent(name, lock) ?: lock
        }) {
            return findClass0(name) ?: throw ClassNotFoundException(name)
        }
    }

    internal fun findClass0(name: String): Class<*>? {
        try {
            return super.findClass(name)
        } catch (notFoundException: ClassNotFoundException) {
            classLoaders.forEach { classLoader ->
                if (classLoader.declaredFilter?.isExported(name) != false) {
                    classLoader.loadClassFromOtherClassLoader(name)?.let { return it }
                }
            }
            throw notFoundException
        }
    }
}

internal class LoadingDeniedException(name: String) : ClassNotFoundException(name)
