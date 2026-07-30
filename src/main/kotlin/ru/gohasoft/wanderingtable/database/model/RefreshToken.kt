package ru.gohasoft.wanderingtable.database.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

/**
 * Выданный refresh-токен (хранится в виде SHA-256 хеша).
 *
 * Первичный ключ — суррогатный [id], а не `userId`: раньше PK был пользователем,
 * из-за чего у него мог существовать только один активный токен, и вход со второго
 * устройства молча завершал сессию на первом.
 */
@Entity
@Table(
    name = "refresh_tokens",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_refresh_tokens_hashed_token", columnNames = ["hashed_token"])
    ],
    indexes = [
        Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
        Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at")
    ]
)
data class RefreshToken(
    @Id val id: ObjectId = ObjectId.get(),
    val userId: ObjectId,
    val expiresAt: Instant,
    val hashedToken: String,
    val createdAt: Instant = nowTruncated()
)
