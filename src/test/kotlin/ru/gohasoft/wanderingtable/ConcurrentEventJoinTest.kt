package ru.gohasoft.wanderingtable

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.gohasoft.wanderingtable.support.ClubFixtures
import ru.gohasoft.wanderingtable.support.runConcurrently

/**
 * Гонки при вступлении в событие. Требуют настоящий PostgreSQL: защита построена на
 * блокировке строки, которую берёт `UPDATE ... WHERE participants_count < max_participants`,
 * и на уникальном индексе (event_id, user_id).
 */
class ConcurrentEventJoinTest : ClubFixtures() {

    @Test
    fun `event never exceeds maxParticipants under concurrent joins`() {
        val gameId = createGame(maxPlayers = 5)
        val creator = registerWithRoles("racecreator@example.com")
        // Создатель уже занимает одно место из пяти, значит свободных ровно четыре.
        val eventId = createRegularGame(gameId, creator, minParticipants = 2, maxParticipants = 5)

        val guests = (1..12).map { registerWithRoles("raceguest$it@example.com") }

        val statuses = runConcurrently(guests.size) { index ->
            postJson(
                "/events/$eventId/join",
                emptyMap<String, Any>(),
                guests[index].accessToken
            ).response.status
        }

        assertThat(statuses.count { it == 200 })
            .describedAs("exactly four free seats, statuses=%s", statuses)
            .isEqualTo(4)
        assertThat(statuses.count { it == 409 }).isEqualTo(8)
        assertThat(participantsCountColumn(eventId)).isEqualTo(5)
        assertThat(participantCountInDb(eventId))
            .describedAs("counter column and participants table must agree")
            .isEqualTo(5)
    }

    @Test
    fun `double click by the same user joins exactly once`() {
        val gameId = createGame(maxPlayers = 5)
        val creator = registerWithRoles("dblcreator@example.com")
        val eventId = createRegularGame(gameId, creator, minParticipants = 2, maxParticipants = 5)
        val guest = registerWithRoles("doubleclicker@example.com")

        val statuses = runConcurrently(6) {
            postJson("/events/$eventId/join", emptyMap<String, Any>(), guest.accessToken)
                .response.status
        }

        assertThat(statuses.count { it == 200 })
            .describedAs("statuses=%s", statuses)
            .isEqualTo(1)
        assertThat(participantCountInDb(eventId)).isEqualTo(2)
        assertThat(participantsCountColumn(eventId))
            .describedAs("rolled back join must not leak a reserved seat")
            .isEqualTo(2)
    }

    @Test
    fun `concurrent start transitions the event exactly once`() {
        val gameId = createGame(name = "Соло-гонка", minPlayers = 1)
        val creator = registerWithRoles("startrace@example.com")
        val eventId = createRegularGame(gameId, creator, minParticipants = 1, maxParticipants = 4)

        val statuses = runConcurrently(6) {
            postJson("/events/$eventId/start", emptyMap<String, Any>(), creator.accessToken)
                .response.status
        }

        assertThat(statuses.count { it == 200 })
            .describedAs("statuses=%s", statuses)
            .isEqualTo(1)
    }

    @Test
    fun `concurrent cancel cancels the event exactly once`() {
        val gameId = createGame()
        val creator = registerWithRoles("cancelrace@example.com")
        val eventId = createRegularGame(gameId, creator)

        val statuses = runConcurrently(6) {
            deleteJson("/events/$eventId", creator.accessToken).response.status
        }

        assertThat(statuses.count { it == 200 })
            .describedAs("statuses=%s", statuses)
            .isEqualTo(1)
    }
}
