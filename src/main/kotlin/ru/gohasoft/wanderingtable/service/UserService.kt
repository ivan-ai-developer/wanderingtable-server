package ru.gohasoft.wanderingtable.service

import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.database.model.Role
import ru.gohasoft.wanderingtable.database.model.User
import ru.gohasoft.wanderingtable.database.repository.UserRepository
import ru.gohasoft.wanderingtable.security.AuthService
import kotlin.jvm.optionals.getOrNull

@Service
class UserService(
    private val userRepository: UserRepository
) {

    fun requireById(userId: ObjectId): User =
        userRepository.findById(userId.value).getOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.")

    /**
     * Поиск участника клуба по email — единственный способ превратить адрес в идентификатор,
     * без которого нельзя выдать роль ([updateRoles] принимает id).
     *
     * Закрыт ролью заведующего: email — персональные данные, и обычному игроку незачем
     * проверять, зарегистрирован ли конкретный адрес в клубе.
     */
    @PreAuthorize("hasRole('CLUB_MANAGER')")
    fun requireByEmail(email: String): User =
        // Через ту же нормализацию, что и регистрация с логином — иначе адрес, сохранённый
        // с заглавными буквами, не нашёлся бы.
        userRepository.findByEmail(AuthService.normalizeEmail(email))
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.")

    @Transactional
    fun updateName(userId: ObjectId, name: String): User {
        val user = requireById(userId)
        user.name = name.trim()
        return userRepository.save(user)
    }

    /**
     * Выдача и снятие ролей. Доступно только заведующему клуба — иначе любой пользователь
     * мог бы выдать себе `TOURNAMENT_CREATOR`, и все остальные проверки прав стали бы
     * декоративными.
     */
    @PreAuthorize("hasRole('CLUB_MANAGER')")
    @Transactional
    fun updateRoles(actorId: ObjectId, targetUserId: ObjectId, roles: Set<Role>): User {
        val target = requireById(targetUserId)

        // PLAYER — базовая роль участника клуба: без неё пользователь не смог бы даже
        // подать заявку на партию, поэтому снять её нельзя.
        val newRoles = roles + Role.PLAYER

        if (target.hasRole(Role.CLUB_MANAGER) && Role.CLUB_MANAGER !in newRoles) {
            if (targetUserId == actorId) {
                throw ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A club manager cannot revoke their own CLUB_MANAGER role."
                )
            }
            if (userRepository.countByRole(Role.CLUB_MANAGER) <= 1) {
                throw ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot revoke the role of the last club manager."
                )
            }
        }

        target.roles = newRoles
        return userRepository.save(target)
    }
}
