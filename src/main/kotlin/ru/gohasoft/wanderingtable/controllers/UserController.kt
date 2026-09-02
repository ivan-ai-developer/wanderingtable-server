package ru.gohasoft.wanderingtable.controllers

import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.gohasoft.wanderingtable.controllers.dto.EventResponse
import ru.gohasoft.wanderingtable.controllers.dto.PageResponse
import ru.gohasoft.wanderingtable.controllers.dto.RegisterDeviceRequest
import ru.gohasoft.wanderingtable.controllers.dto.UnregisterDeviceRequest
import ru.gohasoft.wanderingtable.controllers.dto.UpdateNameRequest
import ru.gohasoft.wanderingtable.controllers.dto.UpdateRolesRequest
import ru.gohasoft.wanderingtable.controllers.dto.UserDeviceResponse
import ru.gohasoft.wanderingtable.controllers.dto.UserProfileResponse
import ru.gohasoft.wanderingtable.controllers.dto.UserResponse
import ru.gohasoft.wanderingtable.controllers.dto.UserStatsResponse
import ru.gohasoft.wanderingtable.controllers.dto.toResponse
import ru.gohasoft.wanderingtable.controllers.utils.getCurrentObjectId
import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.service.GameEventService
import ru.gohasoft.wanderingtable.service.StatsService
import ru.gohasoft.wanderingtable.service.UserDeviceService
import ru.gohasoft.wanderingtable.service.UserService

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
    private val gameEventService: GameEventService,
    private val statsService: StatsService,
    private val userDeviceService: UserDeviceService
) {

    /** История партий пользователя: клубные и турнирные вместе. */
    @GetMapping("/{id}/games")
    fun gameHistory(
        @PathVariable id: String,
        @PageableDefault(size = 20, sort = ["startsAt"], direction = Sort.Direction.DESC)
        pageable: Pageable
    ): PageResponse<EventResponse> =
        gameEventService.history(ObjectId(id), pageable).toResponse { it.toResponse() }

    /**
     * Поиск участника по email — для выдачи ролей: [updateRoles] работает по id, а заведующий
     * знает только адрес. Роли возвращаются в ответе намеренно: `PATCH /{id}/roles` заменяет
     * набор целиком, поэтому клиенту нужен текущий, чтобы добавить роль, а не стереть остальные.
     *
     * Доступно только заведующему клуба — см. [UserService.requireByEmail].
     */
    @GetMapping(params = ["email"])
    fun findByEmail(@RequestParam email: String): UserResponse =
        userService.requireByEmail(email).toResponse()

    /** Профиль текущего пользователя вместе со статистикой — один запрос для клиента. */
    @GetMapping("/me")
    fun me(): UserProfileResponse {
        val userId = getCurrentObjectId()
        return UserProfileResponse(
            user = userService.requireById(userId).toResponse(),
            stats = statsService.statsOf(userId).toResponse()
        )
    }

    /** Сыграно, побед и топ-5 любимых игр — считается запросами, а не хранится в `User`. */
    @GetMapping("/{id}/stats")
    fun stats(@PathVariable id: String): UserStatsResponse =
        statsService.statsOf(ObjectId(id)).toResponse()

    @PatchMapping("/me")
    fun updateName(@Valid @RequestBody body: UpdateNameRequest): UserResponse =
        userService.updateName(getCurrentObjectId(), body.name).toResponse()

    /**
     * Привязка устройства для push-уведомлений. Токен в ответе не возвращается —
     * это учётные данные устройства, а не публичный идентификатор.
     */
    @PutMapping("/me/devices")
    fun registerDevice(@Valid @RequestBody body: RegisterDeviceRequest): UserDeviceResponse =
        userDeviceService.register(getCurrentObjectId(), body.fcmToken, body.platform).toResponse()

    @DeleteMapping("/me/devices")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unregisterDevice(@Valid @RequestBody body: UnregisterDeviceRequest) {
        userDeviceService.unregister(getCurrentObjectId(), body.fcmToken)
    }

    @GetMapping("/me/devices")
    fun devices(): List<UserDeviceResponse> =
        userDeviceService.devicesOf(getCurrentObjectId()).map { it.toResponse() }

    @PatchMapping("/{id}/roles")
    fun updateRoles(
        @PathVariable id: String,
        @Valid @RequestBody body: UpdateRolesRequest
    ): UserResponse =
        userService.updateRoles(
            actorId = getCurrentObjectId(),
            targetUserId = ObjectId(id),
            roles = body.roles
        ).toResponse()
}
