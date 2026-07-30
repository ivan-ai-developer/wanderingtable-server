package ru.gohasoft.wanderingtable

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.gohasoft.wanderingtable.database.model.Role
import ru.gohasoft.wanderingtable.support.IntegrationTestBase
import ru.gohasoft.wanderingtable.support.runConcurrently

class GameIntegrationTest : IntegrationTestBase() {

    private fun gameBody(name: String, resultType: String = "POINTS") = mapOf(
        "name" to name,
        "description" to "Настольная игра",
        "minPlayers" to 2,
        "maxPlayers" to 5,
        "resultType" to resultType
    )

    @Test
    fun `game creator adds a game to the catalogue`() {
        val creator = registerWithRoles("gamecreator@example.com", Role.GAME_CREATOR)

        val result = postJson("/games", gameBody("Каркассон"), creator.accessToken)

        assertThat(result.response.status).isEqualTo(200)
        assertThat(result.readField("name")).isEqualTo("Каркассон")
        assertThat(result.readField("resultType")).isEqualTo("POINTS")
        assertThat(result.readField("creatorId")).isEqualTo(creator.id)
    }

    @Test
    fun `player without GAME_CREATOR cannot add a game`() {
        val player = registerWithRoles("nogamerole@example.com")

        val result = postJson("/games", gameBody("Манчкин"), player.accessToken)

        assertThat(result.response.status).isEqualTo(403)
    }

    @Test
    fun `requires a token`() {
        assertThat(postJson("/games", gameBody("Каркассон")).response.status).isEqualTo(401)
    }

    @Test
    fun `rejects a duplicate game name with 409`() {
        val creator = registerWithRoles("dupgame@example.com", Role.GAME_CREATOR)
        postJson("/games", gameBody("Каркассон"), creator.accessToken)

        val result = postJson("/games", gameBody("Каркассон"), creator.accessToken)

        assertThat(result.response.status).isEqualTo(409)
    }

    @Test
    fun `rejects maxPlayers below minPlayers`() {
        val creator = registerWithRoles("badrange@example.com", Role.GAME_CREATOR)

        val result = postJson(
            "/games",
            gameBody("Кривая игра") + mapOf("minPlayers" to 5, "maxPlayers" to 2),
            creator.accessToken
        )

        assertThat(result.response.status).isEqualTo(400)
    }

    @Test
    fun `rejects an unknown result type with 400`() {
        val creator = registerWithRoles("badresult@example.com", Role.GAME_CREATOR)

        val result = postJson("/games", gameBody("Игра", resultType = "MAGIC"), creator.accessToken)

        assertThat(result.response.status).isEqualTo(400)
    }

    @Test
    fun `lists and searches the catalogue`() {
        val creator = registerWithRoles("catalogue@example.com", Role.GAME_CREATOR)
        postJson("/games", gameBody("Каркассон"), creator.accessToken)
        postJson("/games", gameBody("Манчкин"), creator.accessToken)

        val all = getJson("/games", creator.accessToken)
        assertThat(all.jsonTree().get("totalElements").asLong()).isEqualTo(2)

        val filtered = getJson("/games?name=карк", creator.accessToken)
        assertThat(filtered.jsonTree().get("totalElements").asLong()).isEqualTo(1)
        assertThat(filtered.jsonTree().get("content").get(0).get("name").asString())
            .isEqualTo("Каркассон")
    }

    @Test
    fun `returns 404 for a missing game`() {
        val player = registerWithRoles("missinggame@example.com")

        assertThat(getJson("/games/nope", player.accessToken).response.status).isEqualTo(404)
    }

    /** Гонка: уникальность имени должна держаться на индексе, а не на проверке чтением. */
    @Test
    fun `concurrent creation of the same game name yields exactly one row`() {
        val creator = registerWithRoles("gamerace@example.com", Role.GAME_CREATOR)

        val statuses = runConcurrently(8) {
            postJson("/games", gameBody("Каркассон"), creator.accessToken).response.status
        }

        assertThat(statuses.count { it == 200 })
            .describedAs("statuses=%s", statuses)
            .isEqualTo(1)
        assertThat(jdbcTemplate.queryForObject("select count(*) from games", Long::class.java))
            .isEqualTo(1)
    }
}
