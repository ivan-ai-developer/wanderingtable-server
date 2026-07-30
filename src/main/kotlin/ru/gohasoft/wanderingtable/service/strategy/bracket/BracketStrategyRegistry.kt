package ru.gohasoft.wanderingtable.service.strategy.bracket

import org.springframework.stereotype.Component
import ru.gohasoft.wanderingtable.service.strategy.StrategyRegistry

@Component
class BracketStrategyRegistry(strategies: List<BracketStrategy>) : StrategyRegistry {
    private val byType = strategies.associateBy { it.type }
    fun resolve(type: BracketStrategyType): BracketStrategy =
        byType[type] ?: error("No BracketStrategy registered for $type")
}