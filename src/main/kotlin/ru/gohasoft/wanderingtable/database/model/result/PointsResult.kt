package ru.gohasoft.wanderingtable.database.model.result

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Transient
import ru.gohasoft.wanderingtable.database.model.game.ResultType

/**
 * Результат игры на очки (например, «Каркассон»): победитель определяется максимумом очков.
 */
@Entity
@Table(name = "points_results")
@DiscriminatorValue("POINTS")
class PointsResult : GameResult() {

    @Column(nullable = false)
    var score: Int = 0

    @get:Transient
    override val resultType: ResultType get() = ResultType.POINTS

    override fun resolveOutcome(peers: List<GameResult>): Outcome {
        val scores = peers.filterIsInstance<PointsResult>().map { it.score }
        val best = scores.maxOrNull() ?: return Outcome.WIN
        if (score < best) return Outcome.LOSS
        // Набравших максимум может быть несколько — тогда это ничья.
        return if (scores.count { it == best } > 1) Outcome.DRAW else Outcome.WIN
    }
}
