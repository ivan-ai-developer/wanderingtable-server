package ru.gohasoft.wanderingtable

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.gohasoft.wanderingtable.support.ClubFixtures
import ru.gohasoft.wanderingtable.support.runConcurrently

class GameResultIntegrationTest : ClubFixtures() {

    /** Готовит партию в статусе IN_PROGRESS с двумя участниками. */
    private fun startedGame(
        gameName: String,
        resultType: String,
        creatorEmail: String,
        guestEmail: String
    ): Triple<String, AuthenticatedUser, AuthenticatedUser> {
        val gameId = createGame(name = gameName, resultType = resultType, minPlayers = 2, maxPlayers = 4)
        val creator = registerWithRoles(creatorEmail)
        val guest = registerWithRoles(guestEmail)
        val eventId = createRegularGame(gameId, creator, minParticipants = 2, maxParticipants = 4)
        postJson("/events/$eventId/join", emptyMap<String, Any>(), guest.accessToken)
        postJson("/events/$eventId/start", emptyMap<String, Any>(), creator.accessToken)
        return Triple(eventId, creator, guest)
    }

    @Test
    fun `finishing a points game derives win and loss from the scores`() {
        val (eventId, creator, guest) =
            startedGame("Каркассон", "POINTS", "pts-owner@example.com", "pts-guest@example.com")

        val result = postJson(
            "/events/$eventId/finish",
            mapOf(
                "results" to listOf(
                    mapOf("userId" to creator.id, "score" to 42),
                    mapOf("userId" to guest.id, "score" to 17)
                )
            ),
            creator.accessToken
        )

        assertThat(result.response.status).isEqualTo(200)
        assertThat(result.jsonTree().get("event").get("status").asString()).isEqualTo("FINISHED")

        val outcomes = result.jsonTree().get("results")
            .associate { it.get("userId").asString() to it.get("outcome").asString() }
        assertThat(outcomes[creator.id]).isEqualTo("WIN")
        assertThat(outcomes[guest.id]).isEqualTo("LOSS")
    }

    @Test
    fun `equal scores produce a draw for both players`() {
        val (eventId, creator, guest) =
            startedGame("Ничья", "POINTS", "draw-owner@example.com", "draw-guest@example.com")

        val result = postJson(
            "/events/$eventId/finish",
            mapOf(
                "results" to listOf(
                    mapOf("userId" to creator.id, "score" to 20),
                    mapOf("userId" to guest.id, "score" to 20)
                )
            ),
            creator.accessToken
        )

        assertThat(result.jsonTree().get("results").map { it.get("outcome").asString() })
            .containsExactly("DRAW", "DRAW")
    }

    @Test
    fun `win loss game accepts the declared outcome`() {
        val (eventId, creator, guest) =
            startedGame("Манчкин", "WIN_LOSS", "wl-owner@example.com", "wl-guest@example.com")

        val result = postJson(
            "/events/$eventId/finish",
            mapOf(
                "results" to listOf(
                    mapOf("userId" to creator.id, "outcome" to "LOSS"),
                    mapOf("userId" to guest.id, "outcome" to "WIN")
                )
            ),
            creator.accessToken
        )

        val outcomes = result.jsonTree().get("results")
            .associate { it.get("userId").asString() to it.get("outcome").asString() }
        assertThat(outcomes[creator.id]).isEqualTo("LOSS")
        assertThat(outcomes[guest.id]).isEqualTo("WIN")
    }

    @Test
    fun `placement game treats first place as a win`() {
        val (eventId, creator, guest) =
            startedGame("Гонка", "PLACEMENT", "pl-owner@example.com", "pl-guest@example.com")

        val result = postJson(
            "/events/$eventId/finish",
            mapOf(
                "results" to listOf(
                    mapOf("userId" to creator.id, "place" to 2),
                    mapOf("userId" to guest.id, "place" to 1)
                )
            ),
            creator.accessToken
        )

        val outcomes = result.jsonTree().get("results")
            .associate { it.get("userId").asString() to it.get("outcome").asString() }
        assertThat(outcomes[guest.id]).isEqualTo("WIN")
        assertThat(outcomes[creator.id]).isEqualTo("LOSS")
    }

    /** Тип результата обязан соответствовать типу счёта игры из справочника. */
    @Test
    fun `rejects a score for a win loss game`() {
        val (eventId, creator, guest) =
            startedGame("Строгий", "WIN_LOSS", "strict-owner@example.com", "strict-guest@example.com")

        val result = postJson(
            "/events/$eventId/finish",
            mapOf(
                "results" to listOf(
                    mapOf("userId" to creator.id, "outcome" to "WIN", "score" to 10),
                    mapOf("userId" to guest.id, "outcome" to "LOSS")
                )
            ),
            creator.accessToken
        )

        assertThat(result.response.status).isEqualTo(400)
        assertThat(result.response.contentAsString).contains("score is not applicable")
    }

    @Test
    fun `rejects a missing score for a points game`() {
        val (eventId, creator, guest) =
            startedGame("Очки", "POINTS", "noscore-owner@example.com", "noscore-guest@example.com")

        val result = postJson(
            "/events/$eventId/finish",
            mapOf(
                "results" to listOf(
                    mapOf("userId" to creator.id),
                    mapOf("userId" to guest.id, "score" to 5)
                )
            ),
            creator.accessToken
        )

        assertThat(result.response.status).isEqualTo(400)
        assertThat(result.response.contentAsString).contains("score is required")
    }

    @Test
    fun `rejects results that miss a participant`() {
        val (eventId, creator, _) =
            startedGame("Непокрытый", "POINTS", "miss-owner@example.com", "miss-guest@example.com")

        val result = postJson(
            "/events/$eventId/finish",
            mapOf("results" to listOf(mapOf("userId" to creator.id, "score" to 1))),
            creator.accessToken
        )

        assertThat(result.response.status).isEqualTo(400)
        assertThat(result.response.contentAsString).contains("Missing results for participants")
    }

    @Test
    fun `rejects a result for a non participant`() {
        val (eventId, creator, guest) =
            startedGame("Чужак", "POINTS", "out-owner@example.com", "out-guest@example.com")
        val outsider = registerWithRoles("out-stranger@example.com")

        val result = postJson(
            "/events/$eventId/finish",
            mapOf(
                "results" to listOf(
                    mapOf("userId" to creator.id, "score" to 1),
                    mapOf("userId" to guest.id, "score" to 2),
                    mapOf("userId" to outsider.id, "score" to 3)
                )
            ),
            creator.accessToken
        )

        assertThat(result.response.status).isEqualTo(400)
        assertThat(result.response.contentAsString).contains("not participants")
    }

    @Test
    fun `outsider cannot finish the game`() {
        val (eventId, _, guest) =
            startedGame("Права", "POINTS", "fin-owner@example.com", "fin-guest@example.com")

        val result = postJson(
            "/events/$eventId/finish",
            mapOf("results" to listOf(mapOf("userId" to guest.id, "score" to 1))),
            guest.accessToken
        )

        assertThat(result.response.status).isEqualTo(403)
    }

    @Test
    fun `cannot finish an event that has not started`() {
        val gameId = createGame(name = "Незапущенная", resultType = "POINTS")
        val creator = registerWithRoles("notstarted@example.com")
        val eventId = createRegularGame(gameId, creator, minParticipants = 2, maxParticipants = 4)

        val result = postJson(
            "/events/$eventId/finish",
            mapOf("results" to listOf(mapOf("userId" to creator.id, "score" to 1))),
            creator.accessToken
        )

        assertThat(result.response.status).isEqualTo(409)
    }

    @Test
    fun `finishing twice returns 409 and does not double count statistics`() {
        val (eventId, creator, guest) =
            startedGame("Дважды", "POINTS", "twice-owner@example.com", "twice-guest@example.com")
        val body = mapOf(
            "results" to listOf(
                mapOf("userId" to creator.id, "score" to 10),
                mapOf("userId" to guest.id, "score" to 5)
            )
        )
        postJson("/events/$eventId/finish", body, creator.accessToken)

        val second = postJson("/events/$eventId/finish", body, creator.accessToken)

        assertThat(second.response.status).isEqualTo(409)
        assertThat(getJson("/users/${creator.id}/stats", creator.accessToken)
            .jsonTree().get("gamesPlayed").asLong()).isEqualTo(1)
    }

    /** Гонка: одновременное завершение не должно удвоить статистику. */
    @Test
    fun `concurrent finish succeeds exactly once`() {
        val (eventId, creator, guest) =
            startedGame("Гонка-финиш", "POINTS", "race-owner@example.com", "race-guest@example.com")
        val body = mapOf(
            "results" to listOf(
                mapOf("userId" to creator.id, "score" to 10),
                mapOf("userId" to guest.id, "score" to 5)
            )
        )

        val statuses = runConcurrently(6) {
            postJson("/events/$eventId/finish", body, creator.accessToken).response.status
        }

        assertThat(statuses.count { it == 200 })
            .describedAs("statuses=%s", statuses)
            .isEqualTo(1)
        assertThat(jdbcTemplate.queryForObject("select count(*) from game_results", Long::class.java))
            .isEqualTo(2)
    }

    @Test
    fun `statistics count played games wins and favourite games`() {
        val (firstEvent, creator, guest) =
            startedGame("Каркассон", "POINTS", "stats-owner@example.com", "stats-guest@example.com")
        postJson(
            "/events/$firstEvent/finish",
            mapOf(
                "results" to listOf(
                    mapOf("userId" to creator.id, "score" to 50),
                    mapOf("userId" to guest.id, "score" to 20)
                )
            ),
            creator.accessToken
        )

        val stats = getJson("/users/${creator.id}/stats", creator.accessToken)

        assertThat(stats.response.status).isEqualTo(200)
        val body = stats.jsonTree()
        assertThat(body.get("gamesPlayed").asLong()).isEqualTo(1)
        assertThat(body.get("wins").asLong()).isEqualTo(1)
        assertThat(body.get("losses").asLong()).isEqualTo(0)
        assertThat(body.get("favoriteGames").get(0).get("name").asString()).isEqualTo("Каркассон")
        assertThat(body.get("favoriteGames").get(0).get("playedCount").asLong()).isEqualTo(1)

        val guestStats = getJson("/users/${guest.id}/stats", guest.accessToken).jsonTree()
        assertThat(guestStats.get("wins").asLong()).isEqualTo(0)
        assertThat(guestStats.get("losses").asLong()).isEqualTo(1)
    }

    @Test
    fun `favourite games are capped at five and ordered by play count`() {
        val creator = registerWithRoles("fav-owner@example.com")
        val guest = registerWithRoles("fav-guest@example.com")
        val gameCreator = registerWithRoles("fav-gamecreator@example.com", ru.gohasoft.wanderingtable.database.model.Role.GAME_CREATOR)

        // Шесть разных игр; по первой играем дважды, чтобы она заняла верх списка.
        val gameIds = (1..6).map { index ->
            createGame(name = "Игра $index", resultType = "POINTS", creator = gameCreator)
        }
        val playCounts = listOf(2, 1, 1, 1, 1, 1)

        gameIds.forEachIndexed { index, gameId ->
            repeat(playCounts[index]) {
                val eventId = createRegularGame(gameId, creator, minParticipants = 2, maxParticipants = 4)
                postJson("/events/$eventId/join", emptyMap<String, Any>(), guest.accessToken)
                postJson("/events/$eventId/start", emptyMap<String, Any>(), creator.accessToken)
                postJson(
                    "/events/$eventId/finish",
                    mapOf(
                        "results" to listOf(
                            mapOf("userId" to creator.id, "score" to 10),
                            mapOf("userId" to guest.id, "score" to 1)
                        )
                    ),
                    creator.accessToken
                )
            }
        }

        val favourites = getJson("/users/${creator.id}/stats", creator.accessToken)
            .jsonTree().get("favoriteGames")

        assertThat(favourites.size()).isEqualTo(5)
        assertThat(favourites.get(0).get("name").asString()).isEqualTo("Игра 1")
        assertThat(favourites.get(0).get("playedCount").asLong()).isEqualTo(2)
    }

    @Test
    fun `finished game results are readable`() {
        val (eventId, creator, guest) =
            startedGame("Читаемый", "POINTS", "read-owner@example.com", "read-guest@example.com")
        postJson(
            "/events/$eventId/finish",
            mapOf(
                "results" to listOf(
                    mapOf("userId" to creator.id, "score" to 7),
                    mapOf("userId" to guest.id, "score" to 3)
                )
            ),
            creator.accessToken
        )

        val results = getJson("/events/$eventId/results", creator.accessToken)

        assertThat(results.response.status).isEqualTo(200)
        assertThat(results.jsonTree().size()).isEqualTo(2)
        assertThat(results.jsonTree().map { it.get("resultType").asString() }).containsOnly("POINTS")
    }
}
