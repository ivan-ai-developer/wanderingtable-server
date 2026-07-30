package ru.gohasoft.wanderingtable

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.gohasoft.wanderingtable.support.IntegrationTestBase

class AuthIntegrationTest : IntegrationTestBase() {

    @Test
    fun `registers user with PLAYER role by default`() {
        val result = postJson(
            "/auth/register",
            mapOf("name" to "Ivan", "email" to "ivan@example.com", "password" to DEFAULT_PASSWORD)
        )

        assertThat(result.response.status).isEqualTo(200)
        val body = result.jsonTree()
        assertThat(body.get("email").asString()).isEqualTo("ivan@example.com")
        assertThat(body.get("roles").map { it.asString() }).containsExactly("PLAYER")
    }

    /** Раньше register сохранял email как есть, а login искал по нему же — но без нормализации
     *  регистра, поэтому вход под другим написанием не работал. */
    @Test
    fun `normalizes email case so a user registered with capitals can log in`() {
        register(email = "MiXeD@Example.COM")

        val tokens = login("mixed@example.com")

        assertThat(tokens.accessToken).isNotBlank()
    }

    /** Валидация @Email отрабатывает раньше нормализации, поэтому email с пробелами
     *  отклоняется как некорректный, а не обрезается. */
    @Test
    fun `rejects an email with surrounding whitespace`() {
        val result = postJson(
            "/auth/register",
            mapOf("name" to "Ivan", "email" to "  spaced@example.com  ", "password" to DEFAULT_PASSWORD)
        )

        assertThat(result.response.status).isEqualTo(400)
    }

    @Test
    fun `rejects duplicate email with 409`() {
        register(email = "dup@example.com")

        val result = postJson(
            "/auth/register",
            mapOf("name" to "Other", "email" to "dup@example.com", "password" to DEFAULT_PASSWORD)
        )

        assertThat(result.response.status).isEqualTo(409)
    }

    @Test
    fun `rejects weak password with 400 and a message`() {
        val result = postJson(
            "/auth/register",
            mapOf("name" to "Ivan", "email" to "weak@example.com", "password" to "short")
        )

        assertThat(result.response.status).isEqualTo(400)
        assertThat(result.response.contentAsString).contains("Password must be at least 8 characters")
    }

    /** Регрессия: `BadCredentialsException` не обрабатывался и отдавал 500. */
    @Test
    fun `returns 401 rather than 500 for a wrong password`() {
        register(email = "wrongpass@example.com")

        val result = postJson(
            "/auth/login",
            mapOf("email" to "wrongpass@example.com", "password" to "WrongPassword1")
        )

        assertThat(result.response.status).isEqualTo(401)
    }

    @Test
    fun `returns 401 for an unknown email`() {
        val result = postJson(
            "/auth/login",
            mapOf("email" to "nobody@example.com", "password" to DEFAULT_PASSWORD)
        )

        assertThat(result.response.status).isEqualTo(401)
    }

    @Test
    fun `rotates refresh token and rejects the consumed one`() {
        register(email = "rotate@example.com")
        val first = login("rotate@example.com")

        val refreshed = postJson("/auth/refresh", mapOf("refreshToken" to first.refreshToken))
        assertThat(refreshed.response.status).isEqualTo(200)
        assertThat(refreshed.readField("refreshToken")).isNotEqualTo(first.refreshToken)

        val reused = postJson("/auth/refresh", mapOf("refreshToken" to first.refreshToken))
        assertThat(reused.response.status).isEqualTo(401)
    }

    /** Раньше PK таблицы токенов был userId, из-за чего второй вход убивал первую сессию. */
    @Test
    fun `keeps sessions from two devices independent`() {
        register(email = "twodevices@example.com")
        val phone = login("twodevices@example.com")
        val tablet = login("twodevices@example.com")

        assertThat(postJson("/auth/refresh", mapOf("refreshToken" to phone.refreshToken))
            .response.status).isEqualTo(200)
        assertThat(postJson("/auth/refresh", mapOf("refreshToken" to tablet.refreshToken))
            .response.status).isEqualTo(200)
    }

    @Test
    fun `logout revokes the refresh token and is idempotent`() {
        register(email = "logout@example.com")
        val tokens = login("logout@example.com")

        assertThat(postJson("/auth/logout", mapOf("refreshToken" to tokens.refreshToken))
            .response.status).isEqualTo(204)
        assertThat(postJson("/auth/logout", mapOf("refreshToken" to tokens.refreshToken))
            .response.status).isEqualTo(204)
        assertThat(postJson("/auth/refresh", mapOf("refreshToken" to tokens.refreshToken))
            .response.status).isEqualTo(401)
    }

    @Test
    fun `rejects an access token used as a refresh token`() {
        register(email = "typeconfusion@example.com")
        val tokens = login("typeconfusion@example.com")

        val result = postJson("/auth/refresh", mapOf("refreshToken" to tokens.accessToken))

        assertThat(result.response.status).isEqualTo(401)
    }

    @Test
    fun `protected endpoint requires a token`() {
        assertThat(getJson("/users/me").response.status).isEqualTo(401)
    }

    @Test
    fun `returns the current user profile with roles`() {
        val user = registerWithRoles("profile@example.com")

        val result = getJson("/users/me", user.accessToken)

        assertThat(result.response.status).isEqualTo(200)
        val body = result.jsonTree()
        assertThat(body.get("user").get("id").asString()).isEqualTo(user.id)
        assertThat(body.get("user").get("roles").map { it.asString() }).containsExactly("PLAYER")
        assertThat(body.get("stats").get("gamesPlayed").asLong()).isEqualTo(0)
    }
}
