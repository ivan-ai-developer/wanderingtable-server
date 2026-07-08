package ru.gohasoft.wanderingtable.database.model

import kotlin.random.Random

@JvmInline
value class ObjectId(val value: String) {
    companion object {
        fun get() = ObjectId(Random.nextLong().toHexString())
    }
}
