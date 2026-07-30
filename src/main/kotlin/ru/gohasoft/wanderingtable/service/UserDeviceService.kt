package ru.gohasoft.wanderingtable.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.gohasoft.wanderingtable.database.model.DevicePlatform
import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.database.model.UserDevice
import ru.gohasoft.wanderingtable.database.model.nowTruncated
import ru.gohasoft.wanderingtable.database.repository.UserDeviceRepository

@Service
class UserDeviceService(
    private val userDeviceRepository: UserDeviceRepository
) {

    /**
     * Привязывает FCM-токен к пользователю. Идемпотентно: повторная регистрация того же
     * токена лишь обновляет владельца и время — так снимается привязка к предыдущему
     * пользователю, если устройство перешло к другому человеку.
     */
    @Transactional
    fun register(userId: ObjectId, fcmToken: String, platform: DevicePlatform): UserDevice {
        val token = fcmToken.trim()
        if (token.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "fcmToken can't be blank.")
        }
        val existing = userDeviceRepository.findByFcmToken(token)
        if (existing != null) {
            existing.userId = userId
            existing.platform = platform
            existing.updatedAt = nowTruncated()
            return userDeviceRepository.save(existing)
        }
        return userDeviceRepository.save(
            UserDevice(userId = userId, fcmToken = token, platform = platform)
        )
    }

    @Transactional
    fun unregister(userId: ObjectId, fcmToken: String) {
        userDeviceRepository.unregister(userId.value, fcmToken.trim())
    }

    fun devicesOf(userId: ObjectId): List<UserDevice> =
        userDeviceRepository.findByUserId(userId.value)
}
