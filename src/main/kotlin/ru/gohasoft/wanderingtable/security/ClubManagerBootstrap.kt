package ru.gohasoft.wanderingtable.security

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import ru.gohasoft.wanderingtable.database.model.Role
import ru.gohasoft.wanderingtable.database.repository.UserRepository

/**
 * Выдаёт роль [Role.CLUB_MANAGER] пользователю с настроенным email при старте приложения.
 *
 * Нужен для разрыва замкнутого круга: роли выдаёт только заведующий клуба, а первого
 * заведующего выдать некому. Поэтому здесь роль пишется напрямую через репозиторий,
 * минуя `UserService.updateRoles` с его `@PreAuthorize` — при старте аутентификации нет.
 *
 * Настраивается свойством `app.bootstrap.club-manager-email`; если оно пустое, ничего не делает.
 */
@Component
class ClubManagerBootstrap(
    private val userRepository: UserRepository,
    @param:Value($$"${app.bootstrap.club-manager-email:}") private val clubManagerEmail: String
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (clubManagerEmail.isBlank()) return

        val email = AuthService.normalizeEmail(clubManagerEmail)
        val user = userRepository.findByEmail(email)
        if (user == null) {
            log.warn(
                "app.bootstrap.club-manager-email={} is set, but no such user exists yet. " +
                    "Register that user and restart to grant CLUB_MANAGER.",
                email
            )
            return
        }
        if (user.hasRole(Role.CLUB_MANAGER)) return

        user.roles = user.roles + Role.CLUB_MANAGER
        userRepository.save(user)
        log.info("Granted CLUB_MANAGER to {}", email)
    }
}
