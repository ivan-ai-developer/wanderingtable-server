package ru.gohasoft.wanderingtable.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.database.model.event.Championship
import ru.gohasoft.wanderingtable.database.model.result.GameResult
import ru.gohasoft.wanderingtable.database.repository.ChampionshipStandingRepository
import ru.gohasoft.wanderingtable.service.strategy.elimination.EliminationStrategyRegistry

data class ChampionshipStandingView(
    val userId: String,
    val eliminatedAtRound: Int?
) {
    val stillIn: Boolean get() = eliminatedAtRound == null
}

@Service
class ChampionshipService(
    private val championshipStandingRepository: ChampionshipStandingRepository,
    private val eliminationStrategies: EliminationStrategyRegistry
) {

    /** Создаёт строки состояния для всех участников — до этого никто не выбыл. */
    @Transactional
    fun initStandings(championship: Championship, participantIds: List<String>) {
        participantIds.forEach {
            championshipStandingRepository.ensureExists(
                ObjectId.get().value,
                championship.id.value,
                it
            )
        }
    }

    /** Отмечает выбывших по результатам партии раунда. */
    @Transactional
    fun applyElimination(championship: Championship, round: Int, results: List<GameResult>) {
        val strategy = eliminationStrategies.resolve(championship.eliminationStrategy)
        strategy.eliminated(results).forEach { userId ->
            championshipStandingRepository.ensureExists(
                ObjectId.get().value,
                championship.id.value,
                userId.value
            )
            championshipStandingRepository.markEliminated(
                championship.id.value,
                userId.value,
                round
            )
        }
    }

    fun standings(championship: Championship): List<ChampionshipStandingView> =
        championshipStandingRepository.findByChampionshipId(championship.id.value)
            .map { ChampionshipStandingView(it.userId.value, it.eliminatedAtRound) }
}
