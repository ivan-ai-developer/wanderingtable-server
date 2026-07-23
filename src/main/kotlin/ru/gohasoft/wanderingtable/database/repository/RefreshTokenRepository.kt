package ru.gohasoft.wanderingtable.database.repository

import ru.gohasoft.wanderingtable.database.model.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import ru.gohasoft.wanderingtable.database.model.ObjectId

interface RefreshTokenRepository : JpaRepository<RefreshToken, ObjectId> {
    fun findByUserIdAndHashedToken(userId: ObjectId, hashedToken: String): RefreshToken?
    fun deleteByUserIdAndHashedToken(userId: ObjectId, hashedToken: String)
}