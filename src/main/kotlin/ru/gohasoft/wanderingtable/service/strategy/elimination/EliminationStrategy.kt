package ru.gohasoft.wanderingtable.service.strategy.elimination

import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.database.model.result.GameResult
import ru.gohasoft.wanderingtable.service.strategy.elimination.EliminationStrategyType

/** Правило выбывания из чемпионата по результатам партии. */
interface EliminationStrategy {
    val type: EliminationStrategyType
    fun eliminated(results: List<GameResult>): List<ObjectId>
}