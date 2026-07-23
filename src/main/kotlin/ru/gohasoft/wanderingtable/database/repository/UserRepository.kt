package ru.gohasoft.wanderingtable.database.repository

import ru.gohasoft.wanderingtable.database.model.User
import org.springframework.data.jpa.repository.JpaRepository
import ru.gohasoft.wanderingtable.database.model.ObjectId

interface UserRepository : JpaRepository<User, ObjectId> {
    fun findByEmail(email: String): User?
}