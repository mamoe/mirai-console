/*
 * Copyright 2019-2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AFFERO GENERAL PUBLIC LICENSE version 3 license that can be found via the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 *
 */

/*
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 */

package net.mamoe.mirai.console.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.mamoe.mirai.console.internal.util.SemVersionInternal
import kotlin.math.max
import kotlin.math.min

// X.X.X-AAAAA+BBBBBB

@Serializable
public data class SemVersion(
    @JvmField
    public val mainVersion: IntArray,
    @JvmField
    public val identifier: String? = null,
    @JvmField
    public val metadata: String? = null
) {
    public fun interface RangeChecker {
        public fun check(version: SemVersion): Boolean
    }

    public companion object {
        @JvmStatic
        private fun String.parseMainVersion(): IntArray =
            split('.').map { it.toInt() }.toIntArray()

        @JvmStatic
        public fun parse(version: String): SemVersion {
            var mainVersionEnd: Int = 0
            kotlin.run {
                val iterator = version.iterator()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    if (next == '-' || next == '+') {
                        break
                    }
                    mainVersionEnd++
                }
            }
            var identifier: String? = null
            var metadata: String? = null
            if (mainVersionEnd != version.length) {
                when (version[mainVersionEnd]) {
                    '-' -> {
                        val metadataSplitter = version.indexOf('+', startIndex = mainVersionEnd)
                        if (metadataSplitter == -1) {
                            identifier = version.substring(mainVersionEnd + 1)
                        } else {
                            identifier = version.substring(mainVersionEnd + 1, metadataSplitter)
                            metadata = version.substring(metadataSplitter + 1)
                        }
                    }
                    '+' -> {
                        metadata = version.substring(mainVersionEnd + 1)
                    }
                }
            }
            return SemVersion(
                mainVersion = version.substring(0, mainVersionEnd).also { mainVersion ->
                    if (mainVersion.indexOf('.') == -1) {
                        throw IllegalArgumentException("$mainVersion must has more than one label")
                    }
                    if (mainVersion.last() == '.') {
                        throw IllegalArgumentException("Version string cannot end-with `.`")
                    }
                }.parseMainVersion(),
                identifier = identifier?.also {
                    if (it.isBlank()) {
                        throw IllegalArgumentException("The identifier cannot be blank.")
                    }
                },
                metadata = metadata?.also {
                    if (it.isBlank()) {
                        throw IllegalArgumentException("The metadata cannot be blank.")
                    }
                }
            )
        }

        @JvmStatic
        public fun parseRangeChecker(range: String): RangeChecker {
            return SemVersionInternal.parseRangeChecker(range)
        }
    }

    @Transient
    private var toString: String? = null
    override fun toString(): String {
        return toString ?: kotlin.run {
            buildString {
                mainVersion.joinTo(this, ".")
                identifier?.let { identifier ->
                    append('-')
                    append(identifier)
                }
                metadata?.let { metadata ->
                    append('+')
                    append(metadata)
                }
            }.also { toString = it }
        }
    }

    public fun toStructureString(): String {
        return "SemVersion{mainVersion=${mainVersion.contentToString()}, identifier=$identifier, metadata=$metadata)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SemVersion

        return compareTo(other) == 0 && other.identifier == identifier && other.metadata == metadata
    }

    override fun hashCode(): Int {
        var result = mainVersion.contentHashCode()
        result = 31 * result + (identifier?.hashCode() ?: 0)
        result = 31 * result + (metadata?.hashCode() ?: 0)
        return result
    }

    public operator fun compareTo(version: SemVersion): Int {
        if (version.mainVersion.contentEquals(mainVersion) && identifier == version.identifier) {
            return 0
        }
        fun IntArray.getSafe(index: Int) = getOrElse(index) { 0 }
        for (index in 0 until (max(mainVersion.size, version.mainVersion.size))) {
            val result = mainVersion.getSafe(index).compareTo(version.mainVersion.getSafe(index))
            if (result != 0) return result
        }
        var identifier0 = identifier
        var identifier1 = version.identifier
        if (identifier0 == null || identifier1 == null) {
            return when (identifier0) {
                identifier1 -> { // null == null
                    0
                }
                null -> {
                    -1
                }
                else -> 1
            }
        }
        fun String.getSafe(index: Int) = getOrElse(index) { ' ' }

        // ignored same prefix
        fun getSameSize(s1: String, s2: String): Int {
            val size = min(s1.length, s2.length)
            var realSameSize = 0
            for (index in 0 until size) {
                if (s1[index] != s2[index]) {
                    return realSameSize
                } else {
                    if (!s1[index].isDigit()) {
                        realSameSize = index + 1
                    }
                }
            }
            return realSameSize
        }

        val ignoredSize = getSameSize(identifier0, identifier1)
        identifier0 = identifier0.substring(ignoredSize)
        identifier1 = identifier1.substring(ignoredSize)
        // Multi-chunk comparing
        val chunks0 = identifier0.split('-')
        val chunks1 = identifier1.split('-')
        chunkLoop@ for (index in 0 until (max(chunks0.size, chunks1.size))) {
            val value0 = chunks0.getOrNull(index)
            val value1 = chunks1.getOrNull(index)
            if (value0 == null || value1 == null) {
                // value0 == null && value1 == null is impossible
                return if (value0 == null) {
                    // 0.0.0-0  0.0.0-0-1
                    -1
                } else {
                    // 0.0.0-1  0.0.0-0
                    1
                }
            }
            try {
                val result = value0.toInt().compareTo(value1.toInt())
                if (result != 0) {
                    return result
                }
                continue@chunkLoop
            } catch (ignored: NumberFormatException) {
            }
            try {
                val result = parse(value0).compareTo(parse(value1))
                if (result != 0) {
                    return result
                }
                continue@chunkLoop
            } catch (ignored: Throwable) {
            }
            // compare chars
            for (index0 in 0 until (max(value0.length, value1.length))) {
                val result = value0.getSafe(index0).compareTo(value1.getSafe(index0))
                if (result != 0)
                    return result
            }
        }
        return 0
    }
}

public fun SemVersion.RangeChecker.check(version: String): Boolean = check(SemVersion.parse(version))