package me.haydencheers.simplag.synonyms

import java.util.*

fun <T> List<List<T>>.cartesianProduct(): List<List<T>> {
    val resultLists = ArrayList<List<T>>()

    val min = this.map { it.size }.min() ?: 0

    for (i in 0 until min) {
        val currentRound = mutableListOf<T>()
        for (list in this) {
            currentRound.add(list[i])
        }
        resultLists.add(currentRound)
    }

    return resultLists
}