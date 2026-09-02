package ru.gohasoft.wanderingtable

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.gohasoft.wanderingtable.database.model.Role
import ru.gohasoft.wanderingtable.support.IntegrationTestBase

class RolesIntegrationTest : IntegrationTestBase() {

    @Test
    fun `club manager grants roles`() {
        val manager = registerWithRoles("manager@example.com", Role.CLUB_MANAGER)
        val player = registerWithRoles("player@example.com")

        val result = patchJson(
            "/users/${player.id}/roles",
            mapOf("roles" to listOf("GAME_CREATOR", "NEWS_CREATOR")),
            manager.accessToken
        )

        assertThat(result.response.status).isEqualTo(200)
        assertThat(result.jsonTree().get("roles").map { it.asString() })
            .containsExactlyInAnyOrder("PLAYER", "GAME_CREATOR", "NEWS_CREATOR")
    }

    @Test
    fun `club manager finds a member by email and gets their current roles`() {
        val manager = registerWithRoles("finder@example.com", Role.CLUB_MANAGER)
        val player = registerWithRoles("findme@example.com", Role.NEWS_CREATOR)

        val result = getJson("/users?email=findme@example.com", manager.accessToken)

        assertThat(result.response.status).isEqualTo(200)
        assertThat(result.readField("id")).isEqualTo(player.id)
        // Роли обязаны быть в ответе: без них клиент не сможет добавить роль, не стерев остальные.
        assertThat(result.jsonTree().get("roles").map { it.asString() })
            .containsExactlyInAnyOrder("PLAYER", "NEWS_CREATOR")
    }

    /**
     * Регистр не должен мешать: адрес хранится нормализованным.
     *
     * Пробелы здесь не проверяются — MockMvc не декодирует `%20` в query-строке, и тест ловил бы
     * собственную кодировку, а не поведение сервера. Обрезка идёт через общий `normalizeEmail`,
     * который покрыт в `AuthIntegrationTest`.
     */
    @Test
    fun `email lookup ignores case`() {
        val manager = registerWithRoles("caseinsensitive@example.com", Role.CLUB_MANAGER)
        val player = registerWithRoles("mixedcase@example.com")

        val result = getJson("/users?email=MixedCase@Example.COM", manager.accessToken)

        assertThat(result.response.status).isEqualTo(200)
        assertThat(result.readField("id")).isEqualTo(player.id)
    }

    @Test
    fun `email lookup answers 404 for an address nobody uses`() {
        val manager = registerWithRoles("nobody@example.com", Role.CLUB_MANAGER)

        val result = getJson("/users?email=ghost@example.com", manager.accessToken)

        assertThat(result.response.status).isEqualTo(404)
    }

    /** Email — персональные данные: обычный игрок не должен проверять, кто состоит в клубе. */
    @Test
    fun `plain player cannot look a member up by email`() {
        val player = registerWithRoles("curious@example.com")
        registerWithRoles("private@example.com")

        val result = getJson("/users?email=private@example.com", player.accessToken)

        assertThat(result.response.status).isEqualTo(403)
    }

    /** Ключевая «проверка на дурака»: без неё все остальные проверки прав декоративны. */
    @Test
    fun `plain player cannot grant roles to themselves`() {
        val player = registerWithRoles("selfpromote@example.com")

        val result = patchJson(
            "/users/${player.id}/roles",
            mapOf("roles" to listOf("TOURNAMENT_CREATOR")),
            player.accessToken
        )

        assertThat(result.response.status).isEqualTo(403)
        assertThat(userRepository.findById(player.id).orElseThrow().roles)
            .containsExactly(Role.PLAYER)
    }

    @Test
    fun `plain player cannot grant roles to another user`() {
        val player = registerWithRoles("attacker@example.com")
        val victim = registerWithRoles("victim@example.com")

        val result = patchJson(
            "/users/${victim.id}/roles",
            mapOf("roles" to listOf("CLUB_MANAGER")),
            player.accessToken
        )

        assertThat(result.response.status).isEqualTo(403)
    }

    @Test
    fun `PLAYER role is always retained`() {
        val manager = registerWithRoles("keepsplayer@example.com", Role.CLUB_MANAGER)
        val player = registerWithRoles("target@example.com")

        val result = patchJson(
            "/users/${player.id}/roles",
            mapOf("roles" to listOf("NEWS_CREATOR")),
            manager.accessToken
        )

        assertThat(result.jsonTree().get("roles").map { it.asString() })
            .contains("PLAYER")
    }

    @Test
    fun `club manager cannot revoke their own manager role`() {
        val manager = registerWithRoles("selfdemote@example.com", Role.CLUB_MANAGER)

        val result = patchJson(
            "/users/${manager.id}/roles",
            mapOf("roles" to listOf("PLAYER")),
            manager.accessToken
        )

        assertThat(result.response.status).isEqualTo(409)
        assertThat(userRepository.findById(manager.id).orElseThrow().roles)
            .contains(Role.CLUB_MANAGER)
    }

    @Test
    fun `revoked role stops working immediately without waiting for token expiry`() {
        val manager = registerWithRoles("revoker@example.com", Role.CLUB_MANAGER)
        val creator = registerWithRoles("creator@example.com", Role.NEWS_CREATOR)

        // Токен выдан, когда роль ещё была — но authorities читаются из БД на каждый запрос.
        assertThat(postJson("/notes", mapOf("title" to "Ok", "content" to ""), creator.accessToken)
            .response.status).isEqualTo(200)

        patchJson("/users/${creator.id}/roles", mapOf("roles" to listOf("PLAYER")), manager.accessToken)

        assertThat(postJson("/notes", mapOf("title" to "Denied", "content" to ""), creator.accessToken)
            .response.status).isEqualTo(403)
    }

    @Test
    fun `rejects an unknown role name with 400`() {
        val manager = registerWithRoles("badrole@example.com", Role.CLUB_MANAGER)
        val player = registerWithRoles("badroletarget@example.com")

        val result = patchJson(
            "/users/${player.id}/roles",
            mapOf("roles" to listOf("SUPREME_LEADER")),
            manager.accessToken
        )

        assertThat(result.response.status).isEqualTo(400)
    }

    @Test
    fun `user updates own name`() {
        val user = registerWithRoles("rename@example.com")

        val result = patchJson("/users/me", mapOf("name" to "Новое Имя"), user.accessToken)

        assertThat(result.response.status).isEqualTo(200)
        assertThat(result.readField("name")).isEqualTo("Новое Имя")
    }

    @Test
    fun `rejects a blank name`() {
        val user = registerWithRoles("blankname@example.com")

        val result = patchJson("/users/me", mapOf("name" to "   "), user.accessToken)

        assertThat(result.response.status).isEqualTo(400)
    }
}
