package ru.gohasoft.wanderingtable.database.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

enum class DevicePlatform { ANDROID, IOS, WEB }

/**
 * Устройство пользователя для push-уведомлений (Firebase Cloud Messaging).
 *
 * Отдельная сущность, а не поле в [User]: у игрока может быть телефон и планшет, и один
 * токен на пользователя означал бы, что уведомления приходят только на последнее устройство.
 *
 * Уникальность по [fcmToken] важна практически: при переустановке приложения или передаче
 * устройства токен может достаться другому пользователю, и старая привязка обязана сняться,
 * иначе уведомления уходили бы не тому человеку.
 */
@Entity
@Table(
    name = "user_devices",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_user_devices_fcm_token", columnNames = ["fcm_token"])
    ],
    indexes = [Index(name = "idx_user_devices_user_id", columnList = "user_id")]
)
data class UserDevice(
    @Id val id: ObjectId = ObjectId.get(),
    @Column(name = "user_id", nullable = false) var userId: ObjectId,
    @Column(name = "fcm_token", nullable = false, length = 512) val fcmToken: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var platform: DevicePlatform = DevicePlatform.ANDROID,
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = nowTruncated()
)
