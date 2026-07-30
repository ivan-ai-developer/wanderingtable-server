package ru.gohasoft.wanderingtable.controllers.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import ru.gohasoft.wanderingtable.database.model.event.Championship
import ru.gohasoft.wanderingtable.database.model.event.League
import ru.gohasoft.wanderingtable.database.model.event.SingleTournament
import ru.gohasoft.wanderingtable.database.model.event.SkillLevel
import ru.gohasoft.wanderingtable.database.model.event.TournamentEvent
import ru.gohasoft.wanderingtable.service.ChampionshipStandingView
import ru.gohasoft.wanderingtable.service.LeagueStandingView
import ru.gohasoft.wanderingtable.service.TournamentKind
import ru.gohasoft.wanderingtable.service.strategy.bracket.BracketStrategyType
import ru.gohasoft.wanderingtable.service.strategy.elimination.EliminationStrategyType
import ru.gohasoft.wanderingtable.service.strategy.league.scoring.LeagueScoringStrategyType
import java.math.BigDecimal
import java.time.Instant

data class CreateTournamentRequest(
    /** SINGLE — обычный турнир, CHAMPIONSHIP — с выбыванием, LEAGUE — на набор очков. */
    val kind: TournamentKind,
    @field:NotBlank(message = "gameId can't be blank.")
    val gameId: String,
    @field:NotBlank(message = "Title can't be blank.")
    @field:Size(max = 200, message = "Title must be at most 200 characters long.")
    val title: String,
    @field:Size(max = 2000, message = "Description must be at most 2000 characters long.")
    val description: String = "",
    val startsAt: Instant,
    val endsAt: Instant? = null,
    @field:DecimalMin(value = "0.0", message = "entryFee must not be negative.")
    val entryFee: BigDecimal = BigDecimal.ZERO,
    val expectedSkillLevel: SkillLevel = SkillLevel.BEGINNER,
    @field:Min(value = 2, message = "minParticipants must be at least 2.")
    val minParticipants: Int,
    @field:Min(value = 2, message = "maxParticipants must be at least 2.")
    val maxParticipants: Int,
    val bracketStrategy: BracketStrategyType = BracketStrategyType.ROUND_ROBIN,
    val eliminationStrategy: EliminationStrategyType = EliminationStrategyType.LOSER_ELIMINATION,
    val scoringStrategy: LeagueScoringStrategyType = LeagueScoringStrategyType.LEVEL_WEIGHTED,
    val seasonStart: Instant? = null,
    val seasonEnd: Instant? = null
)

data class CreateLeagueGameRequest(
    @field:Size(min = 2, message = "A league game needs at least 2 participants.")
    val participantIds: List<String>,
    val startsAt: Instant,
    @field:Min(value = 1, message = "durationMinutes must be at least 1.")
    val durationMinutes: Int? = null
)

/** Ответ по турнирному событию: общие поля [EventResponse] плюс специфика соревнования. */
data class TournamentResponse(
    val event: EventResponse,
    val startsAt: Instant,
    val endsAt: Instant?,
    val entryFee: BigDecimal,
    val expectedSkillLevel: SkillLevel,
    val bracketStrategy: BracketStrategyType? = null,
    val eliminationStrategy: EliminationStrategyType? = null,
    val currentRound: Int? = null,
    val scoringStrategy: LeagueScoringStrategyType? = null,
    val seasonStart: Instant? = null,
    val seasonEnd: Instant? = null
)

fun TournamentEvent.toTournamentResponse(participants: List<String>? = null) = TournamentResponse(
    event = toResponse(participants),
    startsAt = startsAt,
    endsAt = endsAt,
    entryFee = entryFee,
    expectedSkillLevel = expectedSkillLevel,
    bracketStrategy = when (this) {
        is SingleTournament -> bracketStrategy
        is Championship -> bracketStrategy
        else -> null
    },
    eliminationStrategy = (this as? Championship)?.eliminationStrategy,
    currentRound = (this as? Championship)?.currentRound,
    scoringStrategy = (this as? League)?.scoringStrategy,
    seasonStart = (this as? League)?.seasonStart,
    seasonEnd = (this as? League)?.seasonEnd
)

data class LeagueStandingResponse(
    val userId: String,
    val points: Int,
    val level: Int
)

data class ChampionshipStandingResponse(
    val userId: String,
    val eliminatedAtRound: Int?,
    val stillIn: Boolean
)

fun LeagueStandingView.toResponse() = LeagueStandingResponse(
    userId = userId,
    points = points,
    level = level
)

fun ChampionshipStandingView.toResponse() = ChampionshipStandingResponse(
    userId = userId,
    eliminatedAtRound = eliminatedAtRound,
    stillIn = stillIn
)
