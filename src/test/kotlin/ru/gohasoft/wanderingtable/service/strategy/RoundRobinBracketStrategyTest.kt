package ru.gohasoft.wanderingtable.service.strategy

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.service.strategy.bracket.impl.RoundRobinBracketStrategy

class RoundRobinBracketStrategyTest {

    private val strategy = RoundRobinBracketStrategy()

    private fun players(count: Int) = (1..count).map { ObjectId("p$it") }

    @Test
    fun `every pair meets exactly once`() {
        val participants = players(6)

        val pairings = strategy.generate(participants)

        val expectedPairs = participants.size * (participants.size - 1) / 2
        assertThat(pairings).hasSize(expectedPairs)

        val pairs = pairings.map { it.participants.toSet() }
        assertThat(pairs).doesNotHaveDuplicates()
        assertThat(pairs.flatten().distinct()).containsExactlyInAnyOrderElementsOf(participants)
    }

    @Test
    fun `pairs inside one round do not share players`() {
        val pairings = strategy.generate(players(8))

        pairings.groupBy { it.round }.forEach { (round, roundPairings) ->
            val playersInRound = roundPairings.flatMap { it.participants }
            assertThat(playersInRound)
                .describedAs("round %s must not reuse a player", round)
                .doesNotHaveDuplicates()
        }
    }

    @Test
    fun `odd number of players leaves one free each round instead of pairing a ghost`() {
        val participants = players(5)

        val pairings = strategy.generate(participants)

        assertThat(pairings).hasSize(5 * 4 / 2)
        assertThat(pairings.flatMap { it.participants }.distinct())
            .containsExactlyInAnyOrderElementsOf(participants)
        pairings.forEach { assertThat(it.participants).hasSize(2) }
    }

    @Test
    fun `two players produce a single round`() {
        val pairings = strategy.generate(players(2))

        assertThat(pairings).hasSize(1)
        assertThat(pairings.single().round).isEqualTo(1)
    }

    @Test
    fun `round numbers are contiguous starting at one`() {
        val pairings = strategy.generate(players(6))

        assertThat(pairings.map { it.round }.distinct().sorted())
            .isEqualTo((1..5).toList())
    }

    @Test
    fun `refuses to build a bracket for a single player`() {
        assertThatThrownBy { strategy.generate(players(1)) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
