package ru.gohasoft.wanderingtable.controllers.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import ru.gohasoft.wanderingtable.database.model.DevicePlatform
import ru.gohasoft.wanderingtable.database.model.UserDevice
import java.time.Instant

data class RegisterDeviceRequest(
    @field:NotBlank(message = "fcmToken can't be blank.")
    @field:Size(max = 512, message = "fcmToken must be at most 512 characters long.")
    val fcmToken: String,
    val platform: DevicePlatform = DevicePlatform.ANDROID
)

data class UnregisterDeviceRequest(
    @field:NotBlank(message = "fcmToken can't be blank.")
    val fcmToken: String
)

data class UserDeviceResponse(
    val id: String,
    val platform: DevicePlatform,
    val updatedAt: Instant
)

fun UserDevice.toResponse() = UserDeviceResponse(
    id = id.value,
    platform = platform,
    updatedAt = updatedAt
)
