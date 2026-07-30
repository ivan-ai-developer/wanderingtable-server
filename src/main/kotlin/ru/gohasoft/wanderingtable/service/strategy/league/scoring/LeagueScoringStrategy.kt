package ru.gohasoft.wanderingtable.service.strategy.league.scoring

import ru.gohasoft.wanderingtable.database.model.result.Outcome

/** Правило начисления очков лиги. */
interface LeagueScoringStrategy {
    val type: LeagueScoringStrategyType

    /** Сколько очков даёт исход игроку текущего уровня. */
    fun pointsFor(outcome: Outcome, level: Int): Int

    /** Уровень выводится из накопленных очков, а не хранится отдельно. */
    fun levelFor(points: Int): Int
}