package ru.gohasoft.wanderingtable.service.strategy.elimination

import ru.gohasoft.wanderingtable.service.strategy.StrategyType

enum class EliminationStrategyType : StrategyType {
    /** Выбывают проигравшие раунда. */
    LOSER_ELIMINATION
}