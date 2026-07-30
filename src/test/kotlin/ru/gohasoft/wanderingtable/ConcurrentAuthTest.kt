package ru.gohasoft.wanderingtable

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.gohasoft.wanderingtable.support.IntegrationTestBase
import ru.gohasoft.wanderingtable.support.runConcurrently

/**
 * Тесты гонок. Требуют настоящий PostgreSQL: на H2 поведение блокировок при
 * `DELETE ... WHERE` и при нарушении уникального индекса отличается, и тесты были бы
 * зелёными там, где реальная БД падает.
 */
class ConcurrentAuthTest : IntegrationTestBase() {

    @Test
    fun `concurrent registrations with the same email create exactly one user`() {
        val email = "race@example.com"

        val statuses = runConcurrently(8) {
            postJson(
                "/auth/register",
                mapOf("name" to "Racer", "email" to email, "password" to DEFAULT_PASSWORD)
            ).response.status
        }

        assertThat(statuses.count { it == 200 })
            .describedAs("exactly one registration must succeed, statuses=%s", statuses)
            .isEqualTo(1)
        assertThat(statuses.count { it == 409 }).isEqualTo(7)
        assertThat(userRepository.count()).isEqualTo(1)
    }

    @Test
    fun `concurrent refresh with the same token succeeds exactly once`() {
        register(email = "tokenrace@example.com")
        val tokens = login("tokenrace@example.com")

        val statuses = runConcurrently(8) {
            postJson("/auth/refresh", mapOf("refreshToken" to tokens.refreshToken)).response.status
        }

        assertThat(statuses.count { it == 200 })
            .describedAs("refresh token must be single-use, statuses=%s", statuses)
            .isEqualTo(1)
        assertThat(statuses.count { it == 401 }).isEqualTo(7)

        // После ротации в таблице ровно одна строка — выданный новый токен.
        val stored = jdbcTemplate.queryForObject(
            "select count(*) from refresh_tokens", Long::class.java
        )
        assertThat(stored).isEqualTo(1)
    }
}
