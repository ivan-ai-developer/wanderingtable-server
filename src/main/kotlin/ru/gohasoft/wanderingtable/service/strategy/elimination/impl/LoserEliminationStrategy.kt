package ru.gohasoft.wanderingtable.service.strategy.elimination.impl

import org.springframework.stereotype.Component
import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.database.model.result.GameResult
import ru.gohasoft.wanderingtable.database.model.result.Outcome
import ru.gohasoft.wanderingtable.service.strategy.elimination.EliminationStrategyType
import ru.gohasoft.wanderingtable.service.strategy.elimination.EliminationStrategy

/** Простейшее правило: выбывает тот, кто проиграл. Ничья никого не выбивает. */
@Component
class LoserEliminationStrategy : EliminationStrategy {

    override val type = EliminationStrategyType.LOSER_ELIMINATION

    override fun eliminated(results: List<GameResult>): List<ObjectId> =
        results.filter { it.outcome == Outcome.LOSS }.map { it.userId }
}