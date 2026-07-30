package ru.gohasoft.wanderingtable

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.gohasoft.wanderingtable.database.model.Role
import ru.gohasoft.wanderingtable.support.ClubFixtures
import java.time.Instant
import java.time.temporal.ChronoUnit

class TournamentIntegrationTest : ClubFixtures() {

    private fun tournamentBody(
        gameId: String,
        kind: String,
        minParticipants: Int = 2,
        maxParticipants: Int = 8,
        title: String = "Турнир клуба"
    ) = mapOf(
        "kind" to kind,
        "gameId" to gameId,
        "title" to title,
        "description" to "Описание",
        "startsAt" to Instant.now().plus(1, ChronoUnit.DAYS).toString(),
        "endsAt" to Instant.now().plus(2, ChronoUnit.DAYS).toString(),
        "entryFee" to "500.00",
        "expectedSkillLevel" to "INTERMEDIATE",
        "minParticipants" to minParticipants,
        "maxParticipants" to maxParticipants
    )

    /** Игра на двоих — круговая сетка ставит игроков попарно. */
    private fun duelGame(name: String, resultType: String = "POINTS") =
        createGame(name = name, resultType = resultType, minPlayers = 2, maxPlayers = 2)

    @Test
    fun `tournament creator creates a single tournament`() {
        val gameId = duelGame("Шахматы")
        val creator = registerWithRoles("tcreator@example.com", Role.TOURNAMENT_CREATOR)

        val result = postJson("/events/tournaments", tournamentBody(gameId, "SINGLE"), creator.accessToken)

        assertThat(result.response.status).isEqualTo(200)
        val body = result.jsonTree()
        assertThat(body.get("event").get("type").asString()).isEqualTo("SINGLE_TOURNAMENT")
        assertThat(body.get("event").get("participantsCount").asInt()).isEqualTo(1)
        assertThat(body.get("entryFee").asDouble()).isEqualTo(500.0)
        assertThat(body.get("expectedSkillLevel").asString()).isEqualTo("INTERMEDIATE")
        assertThat(body.get("bracketStrategy").asString()).isEqualTo("ROUND_ROBIN")
    }

    /** Ключевая «проверка на дурака» из задания. */
    @Test
    fun `player without TOURNAMENT_CREATOR cannot create a tournament`() {
        val gameId = duelGame("Го")
        val player = registerWithRoles("notcreator@example.com")

        val result = postJson("/events/tournaments", tournamentBody(gameId, "SINGLE"), player.accessToken)

        assertThat(result.response.status).isEqualTo(403)
    }

    @Test
    fun `requires a token`() {
        val gameId = duelGame("Нарды")

        assertThat(postJson("/events/tournaments", tournamentBody(gameId, "SINGLE")).response.status)
            .isEqualTo(401)
    }

    @Test
    fun `creates a championship and a league with their own fields`() {
        val gameId = duelGame("Реверси")
        val creator = registerWithRoles("kinds@example.com", Role.TOURNAMENT_CREATOR)

        val championship = postJson(
            "/events/tournaments",
            tournamentBody(gameId, "CHAMPIONSHIP", title = "Чемпионат"),
            creator.accessToken
        ).jsonTree()
        assertThat(championship.get("event").get("type").asString()).isEqualTo("CHAMPIONSHIP")
        assertThat(championship.get("eliminationStrategy").asString()).isEqualTo("LOSER_ELIMINATION")
        assertThat(championship.get("currentRound").asInt()).isEqualTo(0)

        val league = postJson(
            "/events/tournaments",
            tournamentBody(gameId, "LEAGUE", title = "Лига"),
            creator.accessToken
        ).jsonTree()
        assertThat(league.get("event").get("type").asString()).isEqualTo("LEAGUE")
        assertThat(league.get("scoringStrategy").asString()).isEqualTo("LEVEL_WEIGHTED")
        assertThat(league.get("seasonStart").isNull).isFalse()
    }

    @Test
    fun `rejects endsAt before startsAt`() {
        val gameId = duelGame("Уно")
        val creator = registerWithRoles("baddates@example.com", Role.TOURNAMENT_CREATOR)

        val result = postJson(
            "/events/tournaments",
            tournamentBody(gameId, "SINGLE") + mapOf(
                "startsAt" to Instant.now().plus(5, ChronoUnit.DAYS).toString(),
                "endsAt" to Instant.now().plus(1, ChronoUnit.DAYS).toString()
            ),
            creator.accessToken
        )

        assertThat(result.response.status).isEqualTo(400)
    }

    @Test
    fun `tournaments list is filterable and paginated`() {
        val gameId = duelGame("Домино")
        val creator = registerWithRoles("tlist@example.com", Role.TOURNAMENT_CREATOR)
        postJson("/events/tournaments", tournamentBody(gameId, "SINGLE", title = "A"), creator.accessToken)
        postJson("/events/tournaments", tournamentBody(gameId, "LEAGUE", title = "B"), creator.accessToken)

        val all = getJson("/events/tournaments", creator.accessToken)
        assertThat(all.jsonTree().get("totalElements").asLong()).isEqualTo(2)

        val byGame = getJson("/events/tournaments?gameId=$gameId&status=PLANNED", creator.accessToken)
        assertThat(byGame.jsonTree().get("totalElements").asLong()).isEqualTo(2)
    }

    @Test
    fun `bracket pairs every participant and stays disjoint per round`() {
        val gameId = duelGame("Шашки")
        val creator = registerWithRoles("bracket@example.com", Role.TOURNAMENT_CREATOR)
        val tournamentId = postJson(
            "/events/tournaments", tournamentBody(gameId, "SINGLE"), creator.accessToken
        ).jsonTree().get("event").get("id").asString()

        val guests = (1..3).map { registerWithRoles("bracketguest$it@example.com") }
        guests.forEach {
            postJson("/events/$tournamentId/join", emptyMap<String, Any>(), it.accessToken)
        }

        val bracket = postJson(
            "/events/tournaments/$tournamentId/bracket", emptyMap<String, Any>(), creator.accessToken
        )

        assertThat(bracket.response.status).isEqualTo(200)
        // Четверо участников — шесть партий каждый с каждым.
        assertThat(bracket.jsonTree().size()).isEqualTo(6)
        assertThat(bracket.jsonTree().map { it.get("type").asString() }).containsOnly("TOURNAMENT_GAME")
        assertThat(bracket.jsonTree().map { it.get("participantsCount").asInt() }).containsOnly(2)
    }

    @Test
    fun `bracket cannot be generated twice`() {
        val gameId = duelGame("Крестики")
        val creator = registerWithRoles("bracket2@example.com", Role.TOURNAMENT_CREATOR)
        val tournamentId = postJson(
            "/events/tournaments", tournamentBody(gameId, "SINGLE"), creator.accessToken
        ).jsonTree().get("event").get("id").asString()
        postJson(
            "/events/$tournamentId/join", emptyMap<String, Any>(),
            registerWithRoles("bracket2guest@example.com").accessToken
        )
        postJson("/events/tournaments/$tournamentId/bracket", emptyMap<String, Any>(), creator.accessToken)

        val second = postJson(
            "/events/tournaments/$tournamentId/bracket", emptyMap<String, Any>(), creator.accessToken
        )

        assertThat(second.response.status).isEqualTo(409)
    }

    @Test
    fun `bracket requires the minimum number of participants`() {
        val gameId = duelGame("Морской бой")
        val creator = registerWithRoles("bracketmin@example.com", Role.TOURNAMENT_CREATOR)
        val tournamentId = postJson(
            "/events/tournaments",
            tournamentBody(gameId, "SINGLE", minParticipants = 4),
            creator.accessToken
        ).jsonTree().get("event").get("id").asString()

        val result = postJson(
            "/events/tournaments/$tournamentId/bracket", emptyMap<String, Any>(), creator.accessToken
        )

        assertThat(result.response.status).isEqualTo(409)
        assertThat(result.response.contentAsString).contains("Not enough participants")
    }

    /** Круговая сетка ставит пары, поэтому игра должна допускать ровно двух игроков. */
    @Test
    fun `bracket is refused for a game that cannot be played by two`() {
        val gameId = createGame(name = "Только втроём", resultType = "POINTS", minPlayers = 3, maxPlayers = 6)
        val creator = registerWithRoles("bracketwrong@example.com", Role.TOURNAMENT_CREATOR)
        val tournamentId = postJson(
            "/events/tournaments", tournamentBody(gameId, "SINGLE"), creator.accessToken
        ).jsonTree().get("event").get("id").asString()
        postJson(
            "/events/$tournamentId/join", emptyMap<String, Any>(),
            registerWithRoles("bracketwrongguest@example.com").accessToken
        )

        val result = postJson(
            "/events/tournaments/$tournamentId/bracket", emptyMap<String, Any>(), creator.accessToken
        )

        assertThat(result.response.status).isEqualTo(400)
        assertThat(result.response.contentAsString).contains("Bracket pairs two players")
    }

    @Test
    fun `outsider cannot generate a bracket`() {
        val gameId = duelGame("Тавла")
        val creator = registerWithRoles("bracketowner@example.com", Role.TOURNAMENT_CREATOR)
        val tournamentId = postJson(
            "/events/tournaments", tournamentBody(gameId, "SINGLE"), creator.accessToken
        ).jsonTree().get("event").get("id").asString()
        val outsider = registerWithRoles("bracketoutsider@example.com")
        postJson("/events/$tournamentId/join", emptyMap<String, Any>(), outsider.accessToken)

        val result = postJson(
            "/events/tournaments/$tournamentId/bracket", emptyMap<String, Any>(), outsider.accessToken
        )

        assertThat(result.response.status).isEqualTo(403)
    }

    @Test
    fun `bracket generation is not applicable to a league`() {
        val gameId = duelGame("Лига-игра")
        val creator = registerWithRoles("leaguebracket@example.com", Role.TOURNAMENT_CREATOR)
        val leagueId = postJson(
            "/events/tournaments", tournamentBody(gameId, "LEAGUE"), creator.accessToken
        ).jsonTree().get("event").get("id").asString()
        postJson(
            "/events/$leagueId/join", emptyMap<String, Any>(),
            registerWithRoles("leaguebracketguest@example.com").accessToken
        )

        val result = postJson(
            "/events/tournaments/$leagueId/bracket", emptyMap<String, Any>(), creator.accessToken
        )

        assertThat(result.response.status).isEqualTo(409)
    }

    /** Главная причина общего предка GameEvent: турнирные партии идут в общую статистику. */
    @Test
    fun `tournament games count towards player statistics`() {
        val gameId = duelGame("Статистика-дуэль")
        val creator = registerWithRoles("tstats@example.com", Role.TOURNAMENT_CREATOR)
        val tournamentId = postJson(
            "/events/tournaments", tournamentBody(gameId, "SINGLE"), creator.accessToken
        ).jsonTree().get("event").get("id").asString()
        val guest = registerWithRoles("tstatsguest@example.com")
        postJson("/events/$tournamentId/join", emptyMap<String, Any>(), guest.accessToken)

        val bracket = postJson(
            "/events/tournaments/$tournamentId/bracket", emptyMap<String, Any>(), creator.accessToken
        ).jsonTree()
        val gameEventId = bracket.get(0).get("id").asString()

        postJson("/events/$gameEventId/start", emptyMap<String, Any>(), creator.accessToken)
        val finished = postJson(
            "/events/$gameEventId/finish",
            mapOf(
                "results" to listOf(
                    mapOf("userId" to creator.id, "score" to 30),
                    mapOf("userId" to guest.id, "score" to 10)
                )
            ),
            creator.accessToken
        )
        assertThat(finished.response.status).isEqualTo(200)

        val stats = getJson("/users/${creator.id}/stats", creator.accessToken).jsonTree()
        assertThat(stats.get("gamesPlayed").asLong()).isEqualTo(1)
        assertThat(stats.get("wins").asLong()).isEqualTo(1)
        assertThat(stats.get("favoriteGames").get(0).get("name").asString())
            .isEqualTo("Статистика-дуэль")

        val history = getJson("/users/${creator.id}/games", creator.accessToken).jsonTree()
        assertThat(history.get("totalElements").asLong()).isEqualTo(1)
        assertThat(history.get("content").get(0).get("type").asString()).isEqualTo("TOURNAMENT_GAME")
    }

    @Test
    fun `league awards points by outcome and lists standings`() {
        val gameId = duelGame("Лига-дуэль")
        val creator = registerWithRoles("league@example.com", Role.TOURNAMENT_CREATOR)
        val leagueId = postJson(
            "/events/tournaments", tournamentBody(gameId, "LEAGUE"), creator.accessToken
        ).jsonTree().get("event").get("id").asString()
        val guest = registerWithRoles("leagueguest@example.com")
        postJson("/events/$leagueId/join", emptyMap<String, Any>(), guest.accessToken)

        assertThat(getJson("/events/tournaments/$leagueId/standings", creator.accessToken)
            .jsonTree().size()).isEqualTo(0)

        playLeagueGame(leagueId, creator, guest, winnerScore = 30, loserScore = 5)

        val standings = getJson("/events/tournaments/$leagueId/standings", creator.accessToken)
        assertThat(standings.response.status).isEqualTo(200)
        val byUser = standings.jsonTree().associate { it.get("userId").asString() to it }
        assertThat(byUser[creator.id]!!.get("points").asInt())
            .describedAs("win at level 1 is worth the base amount")
            .isEqualTo(5)
        assertThat(byUser[guest.id]!!.get("points").asInt()).isEqualTo(0)
        assertThat(byUser[creator.id]!!.get("level").asInt()).isEqualTo(1)
    }

    @Test
    fun `league points accumulate across games and raise the level`() {
        val gameId = duelGame("Лига-прогресс")
        val creator = registerWithRoles("leagueprog@example.com", Role.TOURNAMENT_CREATOR)
        val leagueId = postJson(
            "/events/tournaments", tournamentBody(gameId, "LEAGUE"), creator.accessToken
        ).jsonTree().get("event").get("id").asString()
        val guest = registerWithRoles("leagueprogguest@example.com")
        postJson("/events/$leagueId/join", emptyMap<String, Any>(), guest.accessToken)

        // Две победы по 5 очков дают 10 очков, что поднимает уровень до второго;
        // третья победа стоит уже меньше — в этом и состоит правило лиги.
        repeat(3) { playLeagueGame(leagueId, creator, guest, winnerScore = 30, loserScore = 1) }

        val standings = getJson("/events/tournaments/$leagueId/standings", creator.accessToken)
        val winner = standings.jsonTree().first { it.get("userId").asString() == creator.id }
        assertThat(winner.get("points").asInt()).isEqualTo(5 + 5 + 4)
        assertThat(winner.get("level").asInt()).isEqualTo(2)
    }

    @Test
    fun `league game rejects a player from outside the league`() {
        val gameId = duelGame("Лига-чужак")
        val creator = registerWithRoles("leagueout@example.com", Role.TOURNAMENT_CREATOR)
        val leagueId = postJson(
            "/events/tournaments", tournamentBody(gameId, "LEAGUE"), creator.accessToken
        ).jsonTree().get("event").get("id").asString()
        val outsider = registerWithRoles("leagueoutsider@example.com")

        val result = postJson(
            "/events/tournaments/$leagueId/games",
            mapOf(
                "participantIds" to listOf(creator.id, outsider.id),
                "startsAt" to Instant.now().toString()
            ),
            creator.accessToken
        )

        assertThat(result.response.status).isEqualTo(400)
        assertThat(result.response.contentAsString).contains("not league participants")
    }

    @Test
    fun `non member cannot schedule a league game`() {
        val gameId = duelGame("Лига-права")
        val creator = registerWithRoles("leagueperm@example.com", Role.TOURNAMENT_CREATOR)
        val leagueId = postJson(
            "/events/tournaments", tournamentBody(gameId, "LEAGUE"), creator.accessToken
        ).jsonTree().get("event").get("id").asString()
        val member = registerWithRoles("leaguepermmember@example.com")
        postJson("/events/$leagueId/join", emptyMap<String, Any>(), member.accessToken)
        val outsider = registerWithRoles("leaguepermout@example.com")

        // Состав партии корректен — отказ должен быть именно по правам, а не по валидации.
        val result = postJson(
            "/events/tournaments/$leagueId/games",
            mapOf(
                "participantIds" to listOf(creator.id, member.id),
                "startsAt" to Instant.now().toString()
            ),
            outsider.accessToken
        )

        assertThat(result.response.status).isEqualTo(403)
    }

    /** Проводит одну партию лиги от создания до подведения итогов. */
    private fun playLeagueGame(
        leagueId: String,
        winner: AuthenticatedUser,
        loser: AuthenticatedUser,
        winnerScore: Int,
        loserScore: Int
    ) {
        val created = postJson(
            "/events/tournaments/$leagueId/games",
            mapOf(
                "participantIds" to listOf(winner.id, loser.id),
                "startsAt" to Instant.now().toString(),
                "durationMinutes" to 45
            ),
            winner.accessToken
        )
        check(created.response.status == 200) {
            "league game creation failed: ${created.response.status} ${created.response.contentAsString}"
        }
        val gameEventId = created.readField("id")

        postJson("/events/$gameEventId/start", emptyMap<String, Any>(), winner.accessToken)
        val finished = postJson(
            "/events/$gameEventId/finish",
            mapOf(
                "results" to listOf(
                    mapOf("userId" to winner.id, "score" to winnerScore),
                    mapOf("userId" to loser.id, "score" to loserScore)
                )
            ),
            winner.accessToken
        )
        check(finished.response.status == 200) {
            "league game finish failed: ${finished.response.status} ${finished.response.contentAsString}"
        }
    }

    @Test
    fun `standings endpoint rejects a non league`() {
        val gameId = duelGame("Не-лига")
        val creator = registerWithRoles("notleague@example.com", Role.TOURNAMENT_CREATOR)
        val tournamentId = postJson(
            "/events/tournaments", tournamentBody(gameId, "SINGLE"), creator.accessToken
        ).jsonTree().get("event").get("id").asString()

        val result = getJson("/events/tournaments/$tournamentId/standings", creator.accessToken)

        assertThat(result.response.status).isEqualTo(400)
    }

    @Test
    fun `championship marks losers as eliminated`() {
        val gameId = duelGame("Чемп-дуэль")
        val creator = registerWithRoles("champ@example.com", Role.TOURNAMENT_CREATOR)
        val championshipId = postJson(
            "/events/tournaments", tournamentBody(gameId, "CHAMPIONSHIP"), creator.accessToken
        ).jsonTree().get("event").get("id").asString()
        val guest = registerWithRoles("champguest@example.com")
        postJson("/events/$championshipId/join", emptyMap<String, Any>(), guest.accessToken)

        val bracket = postJson(
            "/events/tournaments/$championshipId/bracket", emptyMap<String, Any>(), creator.accessToken
        ).jsonTree()
        val gameEventId = bracket.get(0).get("id").asString()

        postJson("/events/$gameEventId/start", emptyMap<String, Any>(), creator.accessToken)
        postJson(
            "/events/$gameEventId/finish",
            mapOf(
                "results" to listOf(
                    mapOf("userId" to creator.id, "score" to 50),
                    mapOf("userId" to guest.id, "score" to 5)
                )
            ),
            creator.accessToken
        )

        val standings = getJson("/events/tournaments/$championshipId/elimination", creator.accessToken)

        assertThat(standings.response.status).isEqualTo(200)
        val byUser = standings.jsonTree().associate { it.get("userId").asString() to it }
        assertThat(byUser[creator.id]!!.get("stillIn").asBoolean()).isTrue()
        assertThat(byUser[guest.id]!!.get("stillIn").asBoolean()).isFalse()
        assertThat(byUser[guest.id]!!.get("eliminatedAtRound").asInt()).isEqualTo(1)
    }

    @Test
    fun `tournament appears in the common club schedule`() {
        val gameId = duelGame("Расписание")
        val creator = registerWithRoles("schedule2@example.com", Role.TOURNAMENT_CREATOR)
        postJson("/events/tournaments", tournamentBody(gameId, "SINGLE"), creator.accessToken)

        val schedule = getJson("/events", creator.accessToken).jsonTree()

        assertThat(schedule.get("totalElements").asLong()).isEqualTo(1)
        assertThat(schedule.get("content").get(0).get("type").asString()).isEqualTo("SINGLE_TOURNAMENT")
    }
}
