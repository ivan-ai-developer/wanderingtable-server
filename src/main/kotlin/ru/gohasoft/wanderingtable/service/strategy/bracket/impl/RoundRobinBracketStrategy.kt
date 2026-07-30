package ru.gohasoft.wanderingtable.service.strategy.bracket.impl

import org.springframework.stereotype.Component
import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.service.strategy.bracket.BracketStrategyType
import ru.gohasoft.wanderingtable.service.strategy.bracket.BracketPairing
import ru.gohasoft.wanderingtable.service.strategy.bracket.BracketStrategy
import kotlin.collections.plusAssign

/**
 * Круговая система: каждый играет с каждым ровно один раз.
 *
 * Раунды формируются классическим круговым методом, поэтому внутри одного раунда пары
 * не пересекаются — это и позволяет чемпионату отсекать проигравших по раундам.
 * При нечётном числе участников добавляется фиктивный игрок, и в каждом раунде кто-то
 * оказывается свободен.
 */
@Component
class RoundRobinBracketStrategy : BracketStrategy {

    override val type = BracketStrategyType.ROUND_ROBIN

    override fun generate(participants: List<ObjectId>): List<BracketPairing> {
        require(participants.size >= MIN_PARTICIPANTS) {
            "Round robin needs at least $MIN_PARTICIPANTS participants."
        }

        var arrangement = participants.toMutableList()
        if (arrangement.size % 2 != 0) {
            arrangement.add(BYE)
        }
        val size = arrangement.size
        val half = size / 2

        val pairings = mutableListOf<BracketPairing>()
        for (round in 1 until size) {
            for (i in 0 until half) {
                val home = arrangement[i]
                val away = arrangement[size - 1 - i]
                if (home != BYE && away != BYE) {
                    pairings += BracketPairing(round, listOf(home, away))
                }
            }
            arrangement = rotateKeepingFirst(arrangement)
        }
        return pairings
    }

    /** Первый участник закреплён, остальные сдвигаются по кругу — это и есть круговой метод. */
    private fun rotateKeepingFirst(arrangement: List<ObjectId>): MutableList<ObjectId> {
        val fixed = arrangement.first()
        val rotating = arrangement.drop(1).toMutableList()
        val last = rotating.removeAt(rotating.lastIndex)
        rotating.add(0, last)
        return (listOf(fixed) + rotating).toMutableList()
    }

    private companion object {
        const val MIN_PARTICIPANTS = 2

        /** Фиктивный участник для нечётного состава; в пары не попадает. */
        val BYE = ObjectId("__bye__")
    }
}