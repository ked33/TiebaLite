package com.huanchengfly.tieba.post.arch

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/** Keeps the first occurrence of each key across both lists, without concatenating them. */
internal inline fun <T, K> List<T>.mergeDistinct(
    other: List<T>,
    keySelector: (T) -> K,
): ImmutableList<T> {
    val seenKeys = HashSet<K>()
    val result = persistentListOf<T>().builder()
    for (item in this) {
        if (seenKeys.add(keySelector(item))) result.add(item)
    }
    for (item in other) {
        if (seenKeys.add(keySelector(item))) result.add(item)
    }
    return result.build()
}

/** Preserves incoming duplicates, matching a filter against the original list only. */
internal inline fun <T, K> ImmutableList<T>.appendNotInOriginal(
    other: List<T>,
    keySelector: (T) -> K,
): ImmutableList<T> {
    val existingKeys = mapTo(HashSet(), keySelector)
    val result = toPersistentList().builder()
    for (item in other) {
        if (keySelector(item) !in existingKeys) result.add(item)
    }
    return result.build()
}
