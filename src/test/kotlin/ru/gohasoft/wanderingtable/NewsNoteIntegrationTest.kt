package ru.gohasoft.wanderingtable

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.gohasoft.wanderingtable.database.model.Role
import ru.gohasoft.wanderingtable.support.IntegrationTestBase

class NewsNoteIntegrationTest : IntegrationTestBase() {

    /** Регрессия: путь создания заметки падал — в `findById` уходил null. */
    @Test
    fun `creates a note without an id`() {
        val author = registerWithRoles("author@example.com", Role.NEWS_CREATOR)

        val result = postJson(
            "/notes",
            mapOf("title" to "Турнир в субботу", "content" to "Ждём всех"),
            author.accessToken
        )

        assertThat(result.response.status).isEqualTo(200)
        assertThat(result.readField("id")).isNotBlank()
        assertThat(result.readField("title")).isEqualTo("Турнир в субботу")
    }

    @Test
    fun `player without NEWS_CREATOR cannot publish news`() {
        val player = registerWithRoles("plainplayer@example.com")

        val result = postJson("/notes", mapOf("title" to "Fake", "content" to ""), player.accessToken)

        assertThat(result.response.status).isEqualTo(403)
    }

    @Test
    fun `updates own note preserving createdAt`() {
        val author = registerWithRoles("updater@example.com", Role.NEWS_CREATOR)
        val created = postJson("/notes", mapOf("title" to "V1", "content" to "a"), author.accessToken)
        val id = created.readField("id")
        val createdAt = created.readField("createdAt")

        val updated = postJson(
            "/notes",
            mapOf("id" to id, "title" to "V2", "content" to "b"),
            author.accessToken
        )

        assertThat(updated.readField("title")).isEqualTo("V2")
        assertThat(updated.readField("createdAt")).isEqualTo(createdAt)
    }

    @Test
    fun `cannot modify someone else's note`() {
        val owner = registerWithRoles("owner@example.com", Role.NEWS_CREATOR)
        val other = registerWithRoles("intruder@example.com", Role.NEWS_CREATOR)
        val id = postJson("/notes", mapOf("title" to "Mine", "content" to ""), owner.accessToken)
            .readField("id")

        val result = postJson(
            "/notes",
            mapOf("id" to id, "title" to "Hijacked", "content" to ""),
            other.accessToken
        )

        assertThat(result.response.status).isEqualTo(403)
    }

    /** Регрессия: удаление чужой заметки возвращало 200, ничего не удаляя. */
    @Test
    fun `deleting someone else's note returns 403`() {
        val owner = registerWithRoles("delowner@example.com", Role.NEWS_CREATOR)
        val other = registerWithRoles("delintruder@example.com", Role.NEWS_CREATOR)
        val id = postJson("/notes", mapOf("title" to "Mine", "content" to ""), owner.accessToken)
            .readField("id")

        val result = deleteJson("/notes/$id", other.accessToken)

        assertThat(result.response.status).isEqualTo(403)
        assertThat(getJson("/notes").jsonTree().get("totalElements").asLong()).isEqualTo(1)
    }

    /** Регрессия: отсутствующая заметка отдавала 500 через IllegalArgumentException. */
    @Test
    fun `deleting a missing note returns 404`() {
        val author = registerWithRoles("del404@example.com", Role.NEWS_CREATOR)

        val result = deleteJson("/notes/does-not-exist", author.accessToken)

        assertThat(result.response.status).isEqualTo(404)
    }

    @Test
    fun `owner deletes own note`() {
        val author = registerWithRoles("selfdelete@example.com", Role.NEWS_CREATOR)
        val id = postJson("/notes", mapOf("title" to "Bye", "content" to ""), author.accessToken)
            .readField("id")

        assertThat(deleteJson("/notes/$id", author.accessToken).response.status).isEqualTo(204)
        assertThat(getJson("/notes").jsonTree().get("totalElements").asLong()).isEqualTo(0)
    }

    @Test
    fun `club manager deletes any note`() {
        val manager = registerWithRoles("newsmanager@example.com", Role.CLUB_MANAGER)
        val author = registerWithRoles("someauthor@example.com", Role.NEWS_CREATOR)
        val id = postJson("/notes", mapOf("title" to "Spam", "content" to ""), author.accessToken)
            .readField("id")

        assertThat(deleteJson("/notes/$id", manager.accessToken).response.status).isEqualTo(204)
    }

    @Test
    fun `news feed is readable without a token and is paginated`() {
        val author = registerWithRoles("feed@example.com", Role.NEWS_CREATOR)
        repeat(3) { i ->
            postJson("/notes", mapOf("title" to "News $i", "content" to ""), author.accessToken)
        }

        val result = getJson("/notes?page=0&size=2")

        assertThat(result.response.status).isEqualTo(200)
        val body = result.jsonTree()
        assertThat(body.get("content").size()).isEqualTo(2)
        assertThat(body.get("totalElements").asLong()).isEqualTo(3)
        assertThat(body.get("totalPages").asInt()).isEqualTo(2)
    }

    @Test
    fun `own notes endpoint requires a token`() {
        assertThat(getJson("/notes/my").response.status).isEqualTo(401)
    }

    @Test
    fun `rejects a blank title`() {
        val author = registerWithRoles("blanktitle@example.com", Role.NEWS_CREATOR)

        val result = postJson("/notes", mapOf("title" to "  ", "content" to ""), author.accessToken)

        assertThat(result.response.status).isEqualTo(400)
    }
}
