package ru.gohasoft.wanderingtable.database.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import ru.gohasoft.wanderingtable.database.model.event.EventStatus
import ru.gohasoft.wanderingtable.database.model.event.TournamentEvent

interface TournamentEventRepository : JpaRepository<TournamentEvent, String> {

    fun findByStatus(status: EventStatus, pageable: Pageable): Page<TournamentEvent>

    fun findByGameId(gameId: String, pageable: Pageable): Page<TournamentEvent>

    fun findByStatusAndGameId(
        status: EventStatus,
        gameId: String,
        pageable: Pageable
    ): Page<TournamentEvent>
}
