package ru.gohasoft.wanderingtable.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.gohasoft.wanderingtable.database.model.event.Championship
import ru.gohasoft.wanderingtable.database.model.event.League
import ru.gohasoft.wanderingtable.database.model.event.TournamentGame
import ru.gohasoft.wanderingtable.database.model.result.GameResult
import ru.gohasoft.wanderingtable.database.repository.TournamentEventRepository
import kotlin.jvm.optionals.getOrNull

/**
 * Продвигает турнирное событие после завершения его партии.
 *
 * Вынесено из [GameEventService] отдельным классом, чтобы завершение партии не знало
 * правил конкретных подтипов соревнований: добавление нового подтипа затрагивает только
 * этот диспетчер.
 */
@Service
class TournamentProgressService(
    private val tournamentEventRepository: TournamentEventRepository,
    private val leagueService: LeagueService,
    private val championshipService: ChampionshipService
) {

    @Transactional
    fun onTournamentGameFinished(game: TournamentGame, results: List<GameResult>) {
        val tournament = tournamentEventRepository
            .findById(game.tournamentEventId.value).getOrNull() ?: return

        when (tournament) {
            is League -> leagueService.awardPoints(tournament, results)
            is Championship -> championshipService.applyElimination(tournament, game.round, results)
            // У обычного турнира отдельного состояния нет: зачёт считается по game_results.
            else -> Unit
        }
    }
}
