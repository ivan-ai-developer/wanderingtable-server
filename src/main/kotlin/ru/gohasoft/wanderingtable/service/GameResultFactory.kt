package ru.gohasoft.wanderingtable.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import ru.gohasoft.wanderingtable.controllers.dto.SubmitResultRequest
import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.database.model.game.ResultType
import ru.gohasoft.wanderingtable.database.model.result.GameResult
import ru.gohasoft.wanderingtable.database.model.result.Outcome
import ru.gohasoft.wanderingtable.database.model.result.PlacementResult
import ru.gohasoft.wanderingtable.database.model.result.PointsResult
import ru.gohasoft.wanderingtable.database.model.result.WinLossResult

/**
 * Собирает подкласс [GameResult], соответствующий типу счёта игры из справочника,
 * и заодно проверяет, что присланы ровно нужные поля.
 *
 * Держит в одном месте всё знание о соответствии `ResultType` → подкласс результата:
 * добавление нового типа счёта затрагивает только этот класс.
 */
@Component
class GameResultFactory {

    fun create(
        resultType: ResultType,
        gameEventId: ObjectId,
        request: SubmitResultRequest
    ): GameResult {
        val userId = ObjectId(request.userId)
        return when (resultType) {
            ResultType.WIN_LOSS -> {
                val declared = request.outcome
                    ?: badRequest("outcome is required for WIN_LOSS games.")
                if (declared == Outcome.UNDEFINED) {
                    badRequest("outcome must be WIN, LOSS or DRAW.")
                }
                rejectExtras(request, allowScore = false, allowPlace = false)
                WinLossResult().apply {
                    this.gameEventId = gameEventId
                    this.userId = userId
                    this.declaredOutcome = declared
                }
            }

            ResultType.POINTS -> {
                val score = request.score ?: badRequest("score is required for POINTS games.")
                rejectExtras(request, allowScore = true, allowPlace = false)
                PointsResult().apply {
                    this.gameEventId = gameEventId
                    this.userId = userId
                    this.score = score
                }
            }

            ResultType.PLACEMENT -> {
                val place = request.place ?: badRequest("place is required for PLACEMENT games.")
                rejectExtras(request, allowScore = false, allowPlace = true)
                PlacementResult().apply {
                    this.gameEventId = gameEventId
                    this.userId = userId
                    this.place = place
                }
            }
        }
    }

    private fun rejectExtras(
        request: SubmitResultRequest,
        allowScore: Boolean,
        allowPlace: Boolean
    ) {
        if (!allowScore && request.score != null) {
            badRequest("score is not applicable to this game's result type.")
        }
        if (!allowPlace && request.place != null) {
            badRequest("place is not applicable to this game's result type.")
        }
    }

    private fun badRequest(reason: String): Nothing =
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, reason)
}
