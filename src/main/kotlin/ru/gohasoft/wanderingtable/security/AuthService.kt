package ru.gohasoft.wanderingtable.security

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.database.model.RefreshToken
import ru.gohasoft.wanderingtable.database.model.Role
import ru.gohasoft.wanderingtable.database.model.User
import ru.gohasoft.wanderingtable.database.model.nowTruncated
import ru.gohasoft.wanderingtable.database.repository.RefreshTokenRepository
import ru.gohasoft.wanderingtable.database.repository.UserRepository
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64

/** Пара токенов доменного уровня; в HTTP-контракт отдаётся как `TokenPairResponse`. */
data class TokenPair(
    val accessToken: String,
    val refreshToken: String
)

@Service
class AuthService(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val hashEncoder: HashEncoder,
    private val refreshTokenRepository: RefreshTokenRepository
) {

    fun register(name: String, email: String, password: String): User {
        val normalizedEmail = normalizeEmail(email)
        if (userRepository.findByEmail(normalizedEmail) != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, DUPLICATE_EMAIL)
        }
        return try {
            userRepository.save(
                User(
                    name = name.trim(),
                    email = normalizedEmail,
                    hashedPassword = hashEncoder.encode(password),
                    roles = setOf(Role.PLAYER)
                )
            )
        } catch (e: DataIntegrityViolationException) {
            // Проверка выше — лишь быстрый путь. Корректность обеспечивает уникальный индекс
            // uk_users_email: при двух одновременных регистрациях сюда попадает проигравший запрос.
            throw ResponseStatusException(HttpStatus.CONFLICT, DUPLICATE_EMAIL)
        }
    }

    fun login(email: String, password: String): TokenPair {
        val user = userRepository.findByEmail(normalizeEmail(email))
            ?: throw BadCredentialsException(INVALID_CREDENTIALS)

        if (!hashEncoder.matches(password, user.hashedPassword)) {
            throw BadCredentialsException(INVALID_CREDENTIALS)
        }

        return issueTokens(user.id)
    }

    @Transactional
    fun refresh(refreshToken: String): TokenPair {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw unauthorized("Invalid refresh token.")
        }

        val userId = jwtService.getUserIdFromToken(refreshToken)
        if (!userRepository.existsById(userId)) {
            throw unauthorized("Invalid refresh token.")
        }

        // Ротация атомарна: consume() — bulk-delete, возвращающий число затронутых строк.
        // PostgreSQL блокирует строку на время транзакции, поэтому из двух одновременных
        // запросов с одним токеном ровно один получит 1, а второй — 0.
        if (refreshTokenRepository.consume(userId, hashToken(refreshToken)) == 0) {
            throw unauthorized("Refresh token not recognized (maybe used or expired?)")
        }

        return issueTokens(ObjectId(userId))
    }

    /** Отзыв одного refresh-токена. Идемпотентен: повторный вызов не ошибка. */
    @Transactional
    fun logout(refreshToken: String) {
        if (!jwtService.validateRefreshToken(refreshToken)) return
        val userId = jwtService.getUserIdFromToken(refreshToken)
        refreshTokenRepository.consume(userId, hashToken(refreshToken))
    }

    /** Удаляет просроченные токены: строки не чистились и таблица росла бесконечно. */
    @Transactional
    fun purgeExpiredTokens(): Int = refreshTokenRepository.deleteExpired(Instant.now())

    private fun issueTokens(userId: ObjectId): TokenPair {
        val accessToken = jwtService.generateAccessToken(userId.value)
        val refreshToken = jwtService.generateRefreshToken(userId.value)
        storeRefreshToken(userId, refreshToken)
        return TokenPair(accessToken = accessToken, refreshToken = refreshToken)
    }

    private fun storeRefreshToken(userId: ObjectId, rawRefreshToken: String) {
        refreshTokenRepository.save(
            RefreshToken(
                userId = userId,
                expiresAt = nowTruncated().plusMillis(jwtService.refreshTokenValidityMs),
                hashedToken = hashToken(rawRefreshToken)
            )
        )
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }

    private fun unauthorized(reason: String) =
        ResponseStatusException(HttpStatus.UNAUTHORIZED, reason)

    companion object {
        private const val DUPLICATE_EMAIL = "A user with that email already exists."
        private const val INVALID_CREDENTIALS = "Invalid credentials."

        /**
         * Единая нормализация email. Раньше `register` искал по `email.trim()`, но сохранял
         * необрезанное значение, а `login` искал необрезанное — в итоге пользователь,
         * зарегистрированный с пробелом или заглавными буквами, не мог войти.
         */
        fun normalizeEmail(email: String): String = email.trim().lowercase()
    }
}
