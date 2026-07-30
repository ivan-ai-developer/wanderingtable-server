package ru.gohasoft.wanderingtable.database.model.result

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.gohasoft.wanderingtable.database.model.ObjectId

/**
 * Вычисление исхода — чистая логика подклассов результата, проверяется без Spring и БД.
 */
class GameResultOutcomeTest {

    private fun points(score: Int) = PointsResult().apply {
        this.userId = ObjectId("u$score")
        this.score = score
    }

    private fun placement(place: Int) = PlacementResult().apply {
        this.userId = ObjectId("p$place")
        this.place = place
    }

    private fun winLoss(declared: Outcome) = WinLossResult().apply {
        this.declaredOutcome = declared
    }

    @Test
    fun `win loss returns the declared outcome unchanged`() {
        val peers = listOf(winLoss(Outcome.WIN), winLoss(Outcome.LOSS))

        assertThat(peers[0].resolveOutcome(peers)).isEqualTo(Outcome.WIN)
        assertThat(peers[1].resolveOutcome(peers)).isEqualTo(Outcome.LOSS)
    }

    @Test
    fun `highest score wins and the others lose`() {
        val winner = points(42)
        val loser = points(17)
        val peers = listOf(winner, loser)

        assertThat(winner.resolveOutcome(peers)).isEqualTo(Outcome.WIN)
        assertThat(loser.resolveOutcome(peers)).isEqualTo(Outcome.LOSS)
    }

    @Test
    fun `equal top scores are a draw`() {
        val first = points(30)
        val second = points(30)
        val third = points(10)
        val peers = listOf(first, second, third)

        assertThat(first.resolveOutcome(peers)).isEqualTo(Outcome.DRAW)
        assertThat(second.resolveOutcome(peers)).isEqualTo(Outcome.DRAW)
        assertThat(third.resolveOutcome(peers)).isEqualTo(Outcome.LOSS)
    }

    @Test
    fun `a single player with points wins`() {
        val solo = points(5)

        assertThat(solo.resolveOutcome(listOf(solo))).isEqualTo(Outcome.WIN)
    }

    @Test
    fun `negative scores are compared correctly`() {
        val better = points(-1)
        val worse = points(-9)
        val peers = listOf(better, worse)

        assertThat(better.resolveOutcome(peers)).isEqualTo(Outcome.WIN)
        assertThat(worse.resolveOutcome(peers)).isEqualTo(Outcome.LOSS)
    }

    @Test
    fun `first place wins and the rest lose`() {
        val first = placement(1)
        val second = placement(2)
        val third = placement(3)
        val peers = listOf(first, second, third)

        assertThat(first.resolveOutcome(peers)).isEqualTo(Outcome.WIN)
        assertThat(second.resolveOutcome(peers)).isEqualTo(Outcome.LOSS)
        assertThat(third.resolveOutcome(peers)).isEqualTo(Outcome.LOSS)
    }

    @Test
    fun `shared first place is a draw`() {
        val firstA = placement(1)
        val firstB = placement(1)
        val peers = listOf(firstA, firstB)

        assertThat(firstA.resolveOutcome(peers)).isEqualTo(Outcome.DRAW)
        assertThat(firstB.resolveOutcome(peers)).isEqualTo(Outcome.DRAW)
    }

    @Test
    fun `outcome starts undefined until the game is finished`() {
        assertThat(points(10).outcome).isEqualTo(Outcome.UNDEFINED)
        assertThat(placement(1).outcome).isEqualTo(Outcome.UNDEFINED)
    }
}
