package ru.gohasoft.wanderingtable.security

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Удаляет просроченные refresh-токены. Раньше поле `expiresAt` записывалось, но никогда
 * не использовалось, и таблица росла бесконечно: по строке на каждый вход и каждую ротацию.
 */
@Component
class RefreshTokenCleanupJob(
    private val authService: AuthService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${app.cleanup.refresh-tokens-interval-ms:3600000}")
    fun purgeExpired() {
        val removed = authService.purgeExpiredTokens()
        if (removed > 0) {
            log.info("Purged {} expired refresh tokens", removed)
        }
    }
}
