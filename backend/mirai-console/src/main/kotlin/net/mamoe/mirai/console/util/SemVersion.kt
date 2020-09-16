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

// X.X.X-AAAAA+BBBBBB

@Serializable
public data class SemVersion(
    public val mainVersion: IntArray,
    public val identifier: String? = null,
    public val metadata: String? = null
) : Comparable<SemVersion> {
    public fun interface RangeRequirement {
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
        public fun parseRangeRequirement(requirement: String): RangeRequirement {
            return SemVersionInternal.parseRangeRequirement(requirement)
        }

        @JvmStatic
        public fun RangeRequirement.check(version: String): Boolean = check(parse(version))

        @JvmStatic
        public fun SemVersion.satisfies(requirement: RangeRequirement): Boolean = requirement.check(this)

        /** for Kotlin only */
        @JvmStatic
        @JvmSynthetic
        public operator fun RangeRequirement.contains(version: SemVersion): Boolean = check(version)

        /** for Kotlin only */
        @JvmStatic
        @JvmSynthetic
        public operator fun RangeRequirement.contains(version: String): Boolean = check(version)
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

    public fun toStructuredString(): String {
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

    /**
     * Compares this object with the specified object for order. Returns zero if this object is equal
     * to the specified [other] object, a negative number if it's less than [other], or a positive number
     * if it's greater than [other].
     */
    public override operator fun compareTo(other: SemVersion): Int {
        return SemVersionInternal.run { compareInternal(other) }
    }
}
