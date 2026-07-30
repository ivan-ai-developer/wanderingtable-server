package ru.gohasoft.wanderingtable.service.strategy.league.scoring

import ru.gohasoft.wanderingtable.service.strategy.StrategyType

enum class LeagueScoringStrategyType : StrategyType {
    /** Чем выше уровень игрока, тем меньше очков он получает за победу. */
    LEVEL_WEIGHTED
}