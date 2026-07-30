package ru.gohasoft.wanderingtable.service.strategy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.database.model.result.Outcome
import ru.gohasoft.wanderingtable.database.model.result.PointsResult
import ru.gohasoft.wanderingtable.service.strategy.elimination.impl.LoserEliminationStrategy

class LoserEliminationStrategyTest {

    private val strategy = LoserEliminationStrategy()

    private fun result(userId: String, outcome: Outcome) = PointsResult().apply {
        this.userId = ObjectId(userId)
        this.outcome = outcome
    }

    @Test
    fun `only the loser is eliminated`() {
        val results = listOf(
            result("winner", Outcome.WIN),
            result("loser", Outcome.LOSS)
        )

        assertThat(strategy.eliminated(results).map { it.value }).containsExactly("loser")
    }

    @Test
    fun `a draw eliminates nobody`() {
        val results = listOf(
            result("a", Outcome.DRAW),
            result("b", Outcome.DRAW)
        )

        assertThat(strategy.eliminated(results)).isEmpty()
    }

    @Test
    fun `several losers are all eliminated`() {
        val results = listOf(
            result("winner", Outcome.WIN),
            result("loser1", Outcome.LOSS),
            result("loser2", Outcome.LOSS)
        )

        assertThat(strategy.eliminated(results).map { it.value })
            .containsExactlyInAnyOrder("loser1", "loser2")
    }

    @Test
    fun `unfinished results eliminate nobody`() {
        val results = listOf(result("a", Outcome.UNDEFINED))

        assertThat(strategy.eliminated(results)).isEmpty()
    }
}
