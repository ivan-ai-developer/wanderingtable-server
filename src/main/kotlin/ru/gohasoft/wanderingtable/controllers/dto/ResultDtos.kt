package ru.gohasoft.wanderingtable.controllers.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import ru.gohasoft.wanderingtable.database.model.game.ResultType
import ru.gohasoft.wanderingtable.database.model.result.GameResult
import ru.gohasoft.wanderingtable.database.model.result.Outcome
import ru.gohasoft.wanderingtable.database.model.result.PlacementResult
import ru.gohasoft.wanderingtable.database.model.result.PointsResult
import ru.gohasoft.wanderingtable.service.FavoriteGame
import ru.gohasoft.wanderingtable.service.UserStats

/**
 * Один результат при завершении партии. Какие поля обязательны, определяет `resultType`
 * игры из справочника; лишние поля отклоняются, чтобы нельзя было прислать очки в игру,
 * где счёт не ведётся.
 */
data class SubmitResultRequest(
    @field:NotBlank(message = "userId can't be blank.")
    val userId: String,
    /** Требуется для `WIN_LOSS`. */
    val outcome: Outcome? = null,
    /** Требуется для `POINTS`. */
    val score: Int? = null,
    /** Требуется для `PLACEMENT`. */
    @field:Min(value = 1, message = "place must be at least 1.")
    val place: Int? = null
)

data class FinishGameEventRequest(
    @field:NotEmpty(message = "results can't be empty.")
    @field:Valid
    val results: List<SubmitResultRequest>
)

data class GameResultResponse(
    val userId: String,
    val resultType: ResultType,
    val outcome: Outcome,
    val score: Int? = null,
    val place: Int? = null
)

fun GameResult.toResponse() = GameResultResponse(
    userId = userId.value,
    resultType = resultType,
    outcome = outcome,
    score = (this as? PointsResult)?.score,
    place = (this as? PlacementResult)?.place
)

data class FinishedGameEventResponse(
    val event: EventResponse,
    val results: List<GameResultResponse>
)

data class FavoriteGameResponse(
    val gameId: String,
    val name: String,
    val playedCount: Long
)

data class UserStatsResponse(
    val userId: String,
    val gamesPlayed: Long,
    val wins: Long,
    val draws: Long,
    val losses: Long,
    val favoriteGames: List<FavoriteGameResponse>
)

fun FavoriteGame.toResponse() = FavoriteGameResponse(
    gameId = gameId,
    name = name,
    playedCount = playedCount
)

fun UserStats.toResponse() = UserStatsResponse(
    userId = userId,
    gamesPlayed = gamesPlayed,
    wins = wins,
    draws = draws,
    losses = losses,
    favoriteGames = favoriteGames.map { it.toResponse() }
)
