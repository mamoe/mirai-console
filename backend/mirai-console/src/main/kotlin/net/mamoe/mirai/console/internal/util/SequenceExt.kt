/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
/*
 * Copyright 2019-2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AFFERO GENERAL PUBLIC LICENSE version 3 license that can be found via the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */

package net.mamoe.mirai.console.internal.util

internal fun <T> Sequence<T>.dropWhileWithFilter(
    filter: (T) -> Boolean,
    predicate: (T) -> Boolean?
) = DropWhileSequence(this, predicate, filter)

internal fun <T> Sequence<T>.mapFirst(
    predicate: (T) -> Boolean,
    transform: (T) -> T
) = TransformFirstSequence(this, predicate, transform)

/**
 * A sequence that skips the values from the underlying [sequence] while the given [predicate] returns `true` and returns
 * all values after that.
 */
internal class DropWhileSequence<T>
constructor(
    private val sequence: Sequence<T>,
    private val predicate: (T) -> Boolean?,
    private val filter: (T) -> Boolean
) : Sequence<T> {

    override fun iterator(): Iterator<T> = object : Iterator<T> {
        val iterator = sequence.iterator()
        var dropState: Int = -1

        // -1 for not dropping,
        // 1 for nextItem,
        // 0 for normal iteration,
        // 3 for nextItem and not dropping
        var nextItem: T? = null

        private fun drop() {
            loop@ while (iterator.hasNext()) {
                val item = iterator.next()
                if (!filter(item)) {
                    nextItem = item
                    dropState = 3
                    return
                }
                // true -> continue drop
                // false -> cancel drop
                // null -> drop and stop
                when (predicate(item)) {
                    true -> {
                    }
                    false -> {
                        nextItem = item
                        dropState = 1
                        return
                    }
                    null -> {
                        dropState = 0
                        return
                    }
                }
            }
            dropState = 0
        }

        override fun next(): T {
            if (dropState == -1)
                drop()

            if (dropState == 1 || dropState == 3) {
                @Suppress("UNCHECKED_CAST")
                val result = nextItem as T
                nextItem = null
                dropState = if (dropState == 3) -1 else 0
                return result
            }
            return iterator.next()
        }

        override fun hasNext(): Boolean {
            if (dropState == -1)
                drop()
            return dropState == 1 || dropState == 3 || iterator.hasNext()
        }
    }
}

internal class TransformFirstSequence<T>
constructor(
    private val sequence: Sequence<T>,
    private val predicate: (T) -> Boolean,
    private val transform: (T) -> T
) : Sequence<T> {
    override fun iterator(): Iterator<T> {
        return object : Iterator<T> {
            val iterator = sequence.iterator()
            var transformed = false
            override fun hasNext(): Boolean {
                return iterator.hasNext()
            }

            override fun next(): T {
                if (transformed) {
                    return iterator.next()
                }
                val next = iterator.next()
                if (predicate(next)) {
                    transformed = true
                    return transform(next)
                }
                return next
            }
        }
    }
}