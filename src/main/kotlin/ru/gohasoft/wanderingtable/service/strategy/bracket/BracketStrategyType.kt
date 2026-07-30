package ru.gohasoft.wanderingtable.service.strategy.bracket

import ru.gohasoft.wanderingtable.service.strategy.StrategyType

enum class BracketStrategyType  : StrategyType {
    /** Каждый с каждым; раунды формируются круговым методом. */
    ROUND_ROBIN
}