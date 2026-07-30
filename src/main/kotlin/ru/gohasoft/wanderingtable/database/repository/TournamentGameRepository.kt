package ru.gohasoft.wanderingtable.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.gohasoft.wanderingtable.database.model.event.TournamentGame

interface TournamentGameRepository : JpaRepository<TournamentGame, String> {

    fun findByTournamentEventIdOrderByRoundAsc(tournamentEventId: String): List<TournamentGame>

    fun countByTournamentEventId(tournamentEventId: String): Long
}
