package ru.gohasoft.wanderingtable.database.model

import java.util.UUID

@JvmInline
value class ObjectId(val value: String) {
    companion object {
        /**
         * Идентификаторы служат первичными ключами пользователей, событий и токенов,
         * поэтому генерируются криптографически стойким источником ([UUID.randomUUID]),
         * а не `kotlin.random.Random`: предсказуемый 64-битный ключ позволял бы угадывать
         * чужие id и был подвержен коллизиям.
         */
        fun get() = ObjectId(UUID.randomUUID().toString())
    }
}
