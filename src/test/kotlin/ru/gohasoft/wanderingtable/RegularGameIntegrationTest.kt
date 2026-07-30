package ru.gohasoft.wanderingtable

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.gohasoft.wanderingtable.support.ClubFixtures
import java.time.Instant
import java.time.temporal.ChronoUnit

class RegularGameIntegrationTest : ClubFixtures() {

    @Test
    fun `player creates a regular game and becomes its first participant`() {
        val gameId = createGame()
        val player = registerWithRoles("rgcreator@example.com")

        val result = postJson(
            "/events/regular-games",
            mapOf(
                "gameId" to gameId,
                "title" to "Партия в субботу",
                "description" to "Собираемся в клубе",
                "startsAt" to Instant.now().plus(1, ChronoUnit.DAYS).toString(),
                "durationMinutes" to 90,
                "minParticipants" to 2,
                "maxParticipants" to 4
            ),
            player.accessToken
        )

        assertThat(result.response.status).isEqualTo(200)
        assertThat(result.readField("type")).isEqualTo("REGULAR_GAME")
        assertThat(result.readField("status")).isEqualTo("PLANNED")
        assertThat(result.jsonTree().get("participantsCount").asInt()).isEqualTo(1)
        assertThat(participantCountInDb(result.readField("id"))).isEqualTo(1)
    }

    @Test
    fun `requires a token`() {
        val gameId = createGame()

        val result = postJson(
            "/events/regular-games",
            mapOf(
                "gameId" to gameId, "title" to "T", "description" to "",
                "startsAt" to Instant.now().toString(),
                "minParticipants" to 2, "maxParticipants" to 4
            )
        )

        assertThat(result.response.status).isEqualTo(401)
    }

    @Test
    fun `rejects a missing game with 404`() {
        val player = registerWithRoles("nogame@example.com")

        val result = postJson(
            "/events/regular-games",
            mapOf(
                "gameId" to "no-such-game", "title" to "T", "description" to "",
                "startsAt" to Instant.now().toString(),
                "minParticipants" to 2, "maxParticipants" to 4
            ),
            player.accessToken
        )

        assertThat(result.response.status).isEqualTo(404)
    }

    @Test
    fun `rejects participant bounds outside the game limits`() {
        val gameId = createGame(name = "Ужин", minPlayers = 2, maxPlayers = 4)
        val player = registerWithRoles("outofbounds@example.com")

        val result = postJson(
            "/events/regular-games",
            mapOf(
                "gameId" to gameId, "title" to "T", "description" to "",
                "startsAt" to Instant.now().toString(),
                "minParticipants" to 2, "maxParticipants" to 12
            ),
            player.accessToken
        )

        assertThat(result.response.status).isEqualTo(400)
        assertThat(result.response.contentAsString).contains("within the game limits")
    }

    @Test
    fun `second player joins`() {
        val gameId = createGame()
        val creator = registerWithRoles("joincreator@example.com")
        val eventId = createRegularGame(gameId, creator)
        val guest = registerWithRoles("guest@example.com")

        val result = postJson("/events/$eventId/join", emptyMap<String, Any>(), guest.accessToken)

        assertThat(result.response.status).isEqualTo(200)
        assertThat(result.jsonTree().get("participantsCount").asInt()).isEqualTo(2)
        assertThat(participantCountInDb(eventId)).isEqualTo(2)
    }

    @Test
    fun `joining twice returns 409 and does not consume a seat`() {
        val gameId = createGame()
        val creator = registerWithRoles("dupjoincreator@example.com")
        val eventId = createRegularGame(gameId, creator)
        val guest = registerWithRoles("dupguest@example.com")
        postJson("/events/$eventId/join", emptyMap<String, Any>(), guest.accessToken)

        val result = postJson("/events/$eventId/join", emptyMap<String, Any>(), guest.accessToken)

        assertThat(result.response.status).isEqualTo(409)
        assertThat(participantsCountColumn(eventId)).isEqualTo(2)
        assertThat(participantCountInDb(eventId)).isEqualTo(2)
    }

    @Test
    fun `joining a full event returns 409`() {
        val gameId = createGame()
        val creator = registerWithRoles("fullcreator@example.com")
        val eventId = createRegularGame(gameId, creator, minParticipants = 2, maxParticipants = 2)
        postJson(
            "/events/$eventId/join", emptyMap<String, Any>(),
            registerWithRoles("second@example.com").accessToken
        )

        val third = registerWithRoles("third@example.com")
        val result = postJson("/events/$eventId/join", emptyMap<String, Any>(), third.accessToken)

        assertThat(result.response.status).isEqualTo(409)
        assertThat(participantsCountColumn(eventId)).isEqualTo(2)
    }

    @Test
    fun `guest leaves and frees a seat`() {
        val gameId = createGame()
        val creator = registerWithRoles("leavecreator@example.com")
        val eventId = createRegularGame(gameId, creator, maxParticipants = 2)
        val guest = registerWithRoles("leaveguest@example.com")
        postJson("/events/$eventId/join", emptyMap<String, Any>(), guest.accessToken)

        assertThat(deleteJson("/events/$eventId/leave", guest.accessToken).response.status)
            .isEqualTo(204)
        assertThat(participantsCountColumn(eventId)).isEqualTo(1)

        val other = registerWithRoles("takesfreedseat@example.com")
        assertThat(postJson("/events/$eventId/join", emptyMap<String, Any>(), other.accessToken)
            .response.status).isEqualTo(200)
    }

    @Test
    fun `creator cannot leave own event`() {
        val gameId = createGame()
        val creator = registerWithRoles("noleavecreator@example.com")
        val eventId = createRegularGame(gameId, creator)

        val result = deleteJson("/events/$eventId/leave", creator.accessToken)

        assertThat(result.response.status).isEqualTo(409)
    }

    @Test
    fun `non participant leaving returns 404`() {
        val gameId = createGame()
        val creator = registerWithRoles("stranger-owner@example.com")
        val eventId = createRegularGame(gameId, creator)
        val stranger = registerWithRoles("stranger@example.com")

        assertThat(deleteJson("/events/$eventId/leave", stranger.accessToken).response.status)
            .isEqualTo(404)
    }

    @Test
    fun `creator starts the event once the minimum is reached`() {
        val gameId = createGame()
        val creator = registerWithRoles("startcreator@example.com")
        val eventId = createRegularGame(gameId, creator, minParticipants = 2)

        assertThat(postJson("/events/$eventId/start", emptyMap<String, Any>(), creator.accessToken)
            .response.status)
            .describedAs("only one participant so far")
            .isEqualTo(409)

        postJson(
            "/events/$eventId/join", emptyMap<String, Any>(),
            registerWithRoles("startguest@example.com").accessToken
        )

        val started = postJson("/events/$eventId/start", emptyMap<String, Any>(), creator.accessToken)
        assertThat(started.response.status).isEqualTo(200)
        assertThat(started.readField("status")).isEqualTo("IN_PROGRESS")
    }

    @Test
    fun `outsider cannot start someone else's event`() {
        val gameId = createGame(name = "Соло-старт", minPlayers = 1)
        val creator = registerWithRoles("ownstart@example.com")
        val eventId = createRegularGame(gameId, creator, minParticipants = 1)
        val outsider = registerWithRoles("outsiderstart@example.com")

        assertThat(postJson("/events/$eventId/start", emptyMap<String, Any>(), outsider.accessToken)
            .response.status).isEqualTo(403)
    }

    @Test
    fun `cannot join an event that already started`() {
        val gameId = createGame(name = "Соло-джойн", minPlayers = 1)
        val creator = registerWithRoles("startedjoin@example.com")
        val eventId = createRegularGame(gameId, creator, minParticipants = 1)
        postJson("/events/$eventId/start", emptyMap<String, Any>(), creator.accessToken)

        val late = registerWithRoles("latecomer@example.com")
        assertThat(postJson("/events/$eventId/join", emptyMap<String, Any>(), late.accessToken)
            .response.status).isEqualTo(409)
    }

    @Test
    fun `creator cancels the event and it stays in history`() {
        val gameId = createGame()
        val creator = registerWithRoles("cancelcreator@example.com")
        val eventId = createRegularGame(gameId, creator)

        val cancelled = deleteJson("/events/$eventId", creator.accessToken)

        assertThat(cancelled.response.status).isEqualTo(200)
        assertThat(cancelled.readField("status")).isEqualTo("CANCELLED")
        assertThat(getJson("/events/$eventId", creator.accessToken).response.status).isEqualTo(200)
    }

    @Test
    fun `single event response lists participants`() {
        val gameId = createGame()
        val creator = registerWithRoles("listparts@example.com")
        val eventId = createRegularGame(gameId, creator)
        val guest = registerWithRoles("listpartsguest@example.com")
        postJson("/events/$eventId/join", emptyMap<String, Any>(), guest.accessToken)

        val result = getJson("/events/$eventId", creator.accessToken)

        assertThat(result.jsonTree().get("participants").map { it.asString() })
            .containsExactlyInAnyOrder(creator.id, guest.id)
    }

    @Test
    fun `schedule filters by status and game`() {
        val gameId = createGame()
        val creator = registerWithRoles("schedule@example.com")
        createRegularGame(gameId, creator, title = "Первая")
        val second = createRegularGame(gameId, creator, title = "Вторая")
        deleteJson("/events/$second", creator.accessToken)

        val planned = getJson("/events?status=PLANNED", creator.accessToken)
        assertThat(planned.jsonTree().get("totalElements").asLong()).isEqualTo(1)

        val byGame = getJson("/events?gameId=$gameId", creator.accessToken)
        assertThat(byGame.jsonTree().get("totalElements").asLong()).isEqualTo(2)
    }

    @Test
    fun `game history returns events the player took part in`() {
        val gameId = createGame()
        val creator = registerWithRoles("historyowner@example.com")
        val eventId = createRegularGame(gameId, creator)
        val guest = registerWithRoles("historyguest@example.com")
        postJson("/events/$eventId/join", emptyMap<String, Any>(), guest.accessToken)
        val outsider = registerWithRoles("historyoutsider@example.com")

        val guestHistory = getJson("/users/${guest.id}/games", guest.accessToken)
        assertThat(guestHistory.jsonTree().get("totalElements").asLong()).isEqualTo(1)

        val outsiderHistory = getJson("/users/${outsider.id}/games", outsider.accessToken)
        assertThat(outsiderHistory.jsonTree().get("totalElements").asLong()).isEqualTo(0)
    }
}
