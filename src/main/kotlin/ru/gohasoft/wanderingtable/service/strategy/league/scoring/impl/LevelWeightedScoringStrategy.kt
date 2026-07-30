package ru.gohasoft.wanderingtable.service.strategy.league.scoring.impl

import org.springframework.stereotype.Component
import ru.gohasoft.wanderingtable.database.model.result.Outcome
import ru.gohasoft.wanderingtable.service.strategy.league.scoring.LeagueScoringStrategyType
import ru.gohasoft.wanderingtable.service.strategy.league.scoring.LeagueScoringStrategy

/**
 * Реализует правило «чем выше уровень, тем сложнее набирать»: цена победы падает с ростом
 * уровня, но не ниже одного очка, поэтому прогресс никогда не останавливается полностью.
 */
@Component
class LevelWeightedScoringStrategy : LeagueScoringStrategy {

    override val type = LeagueScoringStrategyType.LEVEL_WEIGHTED

    override fun pointsFor(outcome: Outcome, level: Int): Int = when (outcome) {
        Outcome.WIN -> (BASE_WIN_POINTS - (level - 1)).coerceAtLeast(MIN_WIN_POINTS)
        Outcome.DRAW -> DRAW_POINTS
        Outcome.LOSS, Outcome.UNDEFINED -> 0
    }

    override fun levelFor(points: Int): Int =
        (1 + points / POINTS_PER_LEVEL).coerceAtMost(MAX_LEVEL)

    private companion object {
        const val BASE_WIN_POINTS = 5
        const val MIN_WIN_POINTS = 1
        const val DRAW_POINTS = 1
        const val POINTS_PER_LEVEL = 10
        const val MAX_LEVEL = 10
    }
}