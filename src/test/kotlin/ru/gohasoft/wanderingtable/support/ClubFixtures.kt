package ru.gohasoft.wanderingtable.support

import ru.gohasoft.wanderingtable.database.model.Role
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Хелперы предметной области: собирают справочник игр и события, чтобы сами тесты
 * читались как сценарий, а не как последовательность HTTP-вызовов.
 */
abstract class ClubFixtures : IntegrationTestBase() {

    protected fun createGame(
        name: String = "Каркассон",
        resultType: String = "POINTS",
        minPlayers: Int = 2,
        maxPlayers: Int = 5,
        creator: AuthenticatedUser = registerWithRoles(
            "gamecreator-${name.hashCode()}@example.com",
            Role.GAME_CREATOR
        )
    ): String {
        val result = postJson(
            "/games",
            mapOf(
                "name" to name,
                "description" to "",
                "minPlayers" to minPlayers,
                "maxPlayers" to maxPlayers,
                "resultType" to resultType
            ),
            creator.accessToken
        )
        check(result.response.status == 200) {
            "createGame failed: ${result.response.status} ${result.response.contentAsString}"
        }
        return result.readField("id")
    }

    protected fun createRegularGame(
        gameId: String,
        creator: AuthenticatedUser,
        minParticipants: Int = 2,
        maxParticipants: Int = 4,
        title: String = "Партия в субботу",
        startsAt: Instant = Instant.now().plus(1, ChronoUnit.DAYS)
    ): String {
        val result = postJson(
            "/events/regular-games",
            mapOf(
                "gameId" to gameId,
                "title" to title,
                "description" to "",
                "startsAt" to startsAt.toString(),
                "durationMinutes" to 90,
                "minParticipants" to minParticipants,
                "maxParticipants" to maxParticipants
            ),
            creator.accessToken
        )
        check(result.response.status == 200) {
            "createRegularGame failed: ${result.response.status} ${result.response.contentAsString}"
        }
        return result.readField("id")
    }

    protected fun participantCountInDb(eventId: String): Long =
        jdbcTemplate.queryForObject(
            "select count(*) from event_participants where event_id = ?",
            Long::class.java,
            eventId
        )!!

    protected fun participantsCountColumn(eventId: String): Int =
        jdbcTemplate.queryForObject(
            "select participants_count from events where id = ?",
            Int::class.java,
            eventId
        )!!
}
