package ru.gohasoft.wanderingtable.service.strategy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.gohasoft.wanderingtable.database.model.result.Outcome
import ru.gohasoft.wanderingtable.service.strategy.league.scoring.impl.LevelWeightedScoringStrategy

class LevelWeightedScoringStrategyTest {

    private val strategy = LevelWeightedScoringStrategy()

    @Test
    fun `a win is worth less as the level grows`() {
        val byLevel = (1..5).map { strategy.pointsFor(Outcome.WIN, it) }

        assertThat(byLevel)
            .describedAs("points per win must decrease monotonically")
            .isEqualTo(byLevel.sortedDescending())
        assertThat(byLevel.first()).isGreaterThan(byLevel.last())
    }

    @Test
    fun `a win never drops below one point`() {
        assertThat(strategy.pointsFor(Outcome.WIN, level = 100)).isEqualTo(1)
    }

    @Test
    fun `a draw always scores one and a loss scores nothing`() {
        assertThat(strategy.pointsFor(Outcome.DRAW, level = 1)).isEqualTo(1)
        assertThat(strategy.pointsFor(Outcome.DRAW, level = 9)).isEqualTo(1)
        assertThat(strategy.pointsFor(Outcome.LOSS, level = 1)).isEqualTo(0)
    }

    @Test
    fun `an unfinished game scores nothing`() {
        assertThat(strategy.pointsFor(Outcome.UNDEFINED, level = 1)).isEqualTo(0)
    }

    @Test
    fun `level is derived from points and starts at one`() {
        assertThat(strategy.levelFor(0)).isEqualTo(1)
        assertThat(strategy.levelFor(9)).isEqualTo(1)
        assertThat(strategy.levelFor(10)).isEqualTo(2)
        assertThat(strategy.levelFor(25)).isEqualTo(3)
    }

    @Test
    fun `level is capped so scoring never becomes impossible`() {
        assertThat(strategy.levelFor(100_000)).isEqualTo(10)
        assertThat(strategy.pointsFor(Outcome.WIN, strategy.levelFor(100_000))).isEqualTo(1)
    }
}
