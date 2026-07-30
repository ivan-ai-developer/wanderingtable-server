package ru.gohasoft.wanderingtable.service

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.database.model.result.Outcome
import ru.gohasoft.wanderingtable.database.repository.GameRepository
import ru.gohasoft.wanderingtable.database.repository.GameResultRepository

/** Любимая игра игрока: запись справочника и число сыгранных по ней партий. */
data class FavoriteGame(
    val gameId: String,
    val name: String,
    val playedCount: Long
)

data class UserStats(
    val userId: String,
    val gamesPlayed: Long,
    val wins: Long,
    val draws: Long,
    val losses: Long,
    val favoriteGames: List<FavoriteGame>
)

/**
 * Статистика игрока считается запросами при чтении, а не хранится счётчиками в `User`.
 *
 * Так исключены и рассогласование при откате результата, и гонка при одновременном
 * завершении двух партий одного игрока — счётчик было бы нужно инкрементировать атомарно
 * и пересчитывать при любой правке истории.
 */
@Service
class StatsService(
    private val gameResultRepository: GameResultRepository,
    private val gameRepository: GameRepository
) {

    fun statsOf(userId: ObjectId): UserStats {
        val id = userId.value
        return UserStats(
            userId = id,
            gamesPlayed = gameResultRepository.countPlayed(id),
            wins = gameResultRepository.countByOutcome(id, Outcome.WIN),
            draws = gameResultRepository.countByOutcome(id, Outcome.DRAW),
            losses = gameResultRepository.countByOutcome(id, Outcome.LOSS),
            favoriteGames = favoriteGames(id)
        )
    }

    /** Топ-5 игр по числу сыгранных партий. */
    private fun favoriteGames(userId: String): List<FavoriteGame> {
        val counts = gameResultRepository.findTopGames(userId, PageRequest.of(0, FAVORITE_GAMES_LIMIT))
        if (counts.isEmpty()) return emptyList()

        val namesById = gameRepository.findAllById(counts.map { it.gameId })
            .associate { it.id.value to it.name }

        return counts.map {
            FavoriteGame(
                gameId = it.gameId,
                name = namesById[it.gameId] ?: "",
                playedCount = it.played
            )
        }
    }

    private companion object {
        const val FAVORITE_GAMES_LIMIT = 5
    }
}
