package ru.gohasoft.wanderingtable.service.strategy.elimination

import org.springframework.stereotype.Component
import ru.gohasoft.wanderingtable.service.strategy.StrategyRegistry

@Component
class EliminationStrategyRegistry(strategies: List<EliminationStrategy>) : StrategyRegistry {
    private val byType = strategies.associateBy { it.type }
    fun resolve(type: EliminationStrategyType): EliminationStrategy =
        byType[type] ?: error("No EliminationStrategy registered for $type")
}