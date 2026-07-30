package ru.gohasoft.wanderingtable.controllers.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import ru.gohasoft.wanderingtable.database.model.Role
import ru.gohasoft.wanderingtable.database.model.User

data class UpdateNameRequest(
    @field:NotBlank(message = "Name can't be blank.")
    @field:Size(max = 64, message = "Name must be at most 64 characters long.")
    val name: String
)

data class UpdateRolesRequest(
    @field:NotEmpty(message = "Roles can't be empty.")
    val roles: Set<Role>
)

data class UserResponse(
    val id: String,
    val name: String,
    val email: String,
    val roles: Set<Role>
)

/** Профиль вместе со статистикой, чтобы клиент открывал экран профиля одним запросом. */
data class UserProfileResponse(
    val user: UserResponse,
    val stats: UserStatsResponse
)

fun User.toResponse() = UserResponse(
    id = id.value,
    name = name,
    email = email,
    roles = roles
)
