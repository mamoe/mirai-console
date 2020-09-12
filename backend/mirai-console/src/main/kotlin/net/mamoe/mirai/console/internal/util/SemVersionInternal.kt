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

package net.mamoe.mirai.console.internal.util

import net.mamoe.mirai.console.util.SemVersion

@Suppress("RegExpRedundantEscape")
internal object SemVersionInternal {
    private val directVersion = """^[0-9]+(\.[0-9]+)*$""".toRegex()
    private val versionSelect = """^[0-9]+(\.[0-9]+)*\.x(\.[0-9]+)*$""".toRegex()
    private val versionRange = """([0-9]+(\.[0-9]+)*)\s*\-\s*([0-9]+(\.[0-9]+)*)""".toRegex()
    private val versionMathRange = """\[([0-9]+(\.[0-9]+)*)\s*\,\s*([0-9]+(\.[0-9]+)*)\]""".toRegex()
    private val versionRule = """^*((\>\=)|(\<\=)|(\=)|(\>)|(\<))\s*([0-9]+(\.[0-9]+)*)$""".toRegex()
    private fun Collection<*>.dump() {
        forEachIndexed { index, value ->
            println("$index, $value")
        }
    }

    private fun String.parseRule(): SemVersion.RangeRequirement {
        val trimmed = trim()
        if (directVersion.matches(trimmed)) {
            val parsed = SemVersion.parse(trimmed)
            return SemVersion.RangeRequirement {
                it.compareTo(parsed) == 0
            }
        }
        if (versionSelect.matches(trimmed)) {
            val regex = ("^" +
                    trimmed.replace(".", "\\.")
                        .replace("x", ".+") +
                    "$"
                    ).toRegex()
            return SemVersion.RangeRequirement {
                regex.matches(it.toString())
            }
        }
        (versionRange.matchEntire(trimmed) ?: versionMathRange.matchEntire(trimmed))?.let { range ->
            var start = SemVersion.parse(range.groupValues[1])
            var end = SemVersion.parse(range.groupValues[3])
            if (start > end) {
                val c = end
                end = start
                start = c
            }
            return SemVersion.RangeRequirement {
                start <= it && it <= end
            }
        }
        versionRule.matchEntire(trimmed)?.let { result ->
            val operator = result.groupValues[1]
            val version = SemVersion.parse(result.groupValues[7])
            return when (operator) {
                ">=" -> {
                    SemVersion.RangeRequirement { it >= version }
                }
                ">" -> {
                    SemVersion.RangeRequirement { it > version }
                }
                "<=" -> {
                    SemVersion.RangeRequirement { it <= version }
                }
                "<" -> {
                    SemVersion.RangeRequirement { it < version }
                }
                "=" -> {
                    SemVersion.RangeRequirement { it.compareTo(version) == 0 }
                }
                else -> throw AssertionError("operator=$operator, version=$version")
            }
        }
        throw UnsupportedOperationException("Cannot parse $this")
    }

    fun parseRangeRequirement(requirement: String): SemVersion.RangeRequirement {
        if (requirement.isBlank()) {
            throw IllegalArgumentException("Invalid requirement: Empty requirement rule.")
        }
        return requirement.split("||").map {
            it.parseRule()
        }.let { checks ->
            SemVersion.RangeRequirement {
                checks.forEach { rule ->
                    if (rule.check(it)) return@RangeRequirement true
                }
                return@RangeRequirement false
            }
        }
    }
}