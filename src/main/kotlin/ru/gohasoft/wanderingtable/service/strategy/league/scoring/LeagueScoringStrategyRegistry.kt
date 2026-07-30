package ru.gohasoft.wanderingtable.service.strategy.league.scoring

import org.springframework.stereotype.Component
import ru.gohasoft.wanderingtable.service.strategy.StrategyRegistry

@Component
class LeagueScoringStrategyRegistry(strategies: List<LeagueScoringStrategy>) : StrategyRegistry {
    private val byType = strategies.associateBy { it.type }
    fun resolve(type: LeagueScoringStrategyType): LeagueScoringStrategy =
        byType[type] ?: error("No LeagueScoringStrategy registered for $type")
}