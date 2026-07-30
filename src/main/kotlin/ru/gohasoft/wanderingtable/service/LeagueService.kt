package ru.gohasoft.wanderingtable.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.database.model.event.League
import ru.gohasoft.wanderingtable.database.model.result.GameResult
import ru.gohasoft.wanderingtable.database.repository.LeagueStandingRepository
import ru.gohasoft.wanderingtable.service.strategy.league.scoring.LeagueScoringStrategyRegistry

/** Строка таблицы лиги для выдачи наружу: уровень выводится из очков. */
data class LeagueStandingView(
    val userId: String,
    val points: Int,
    val level: Int
)

@Service
class LeagueService(
    private val leagueStandingRepository: LeagueStandingRepository,
    private val scoringStrategies: LeagueScoringStrategyRegistry
) {

    /**
     * Начисляет очки за завершённую партию лиги.
     *
     * Уровень для расчёта берётся из текущих очков. Два одновременных начисления могут
     * рассчитать вес по одному и тому же уровню — на итоговую сумму это не влияет,
     * поскольку само изменение очков атомарно, а уровень всегда выводится из суммы.
     */
    @Transactional
    fun awardPoints(league: League, results: List<GameResult>) {
        val strategy = scoringStrategies.resolve(league.scoringStrategy)
        val leagueId = league.id.value

        results.forEach { result ->
            val userId = result.userId.value
            leagueStandingRepository.ensureExists(ObjectId.get().value, leagueId, userId)

            val currentPoints = leagueStandingRepository
                .findByLeagueIdAndUserId(leagueId, userId)?.points ?: 0
            val delta = strategy.pointsFor(result.outcome, strategy.levelFor(currentPoints))
            if (delta != 0) {
                leagueStandingRepository.incrementPoints(leagueId, userId, delta)
            }
        }
    }

    fun standings(league: League): List<LeagueStandingView> {
        val strategy = scoringStrategies.resolve(league.scoringStrategy)
        return leagueStandingRepository.findByLeagueIdOrderByPointsDesc(league.id.value)
            .map {
                LeagueStandingView(
                    userId = it.userId.value,
                    points = it.points,
                    level = strategy.levelFor(it.points)
                )
            }
    }
}
