package ru.gohasoft.wanderingtable.database.model.result

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Transient
import ru.gohasoft.wanderingtable.database.model.game.ResultType

/**
 * Результат игры, где фиксируется только итоговое место. Первое место — победа;
 * если первое место разделили, это ничья.
 */
@Entity
@Table(name = "placement_results")
@DiscriminatorValue("PLACEMENT")
class PlacementResult : GameResult() {

    @Column(nullable = false)
    var place: Int = 0

    @get:Transient
    override val resultType: ResultType get() = ResultType.PLACEMENT

    override fun resolveOutcome(peers: List<GameResult>): Outcome {
        if (place != FIRST_PLACE) return Outcome.LOSS
        val firstPlaces = peers.filterIsInstance<PlacementResult>().count { it.place == FIRST_PLACE }
        return if (firstPlaces > 1) Outcome.DRAW else Outcome.WIN
    }

    private companion object {
        const val FIRST_PLACE = 1
    }
}
