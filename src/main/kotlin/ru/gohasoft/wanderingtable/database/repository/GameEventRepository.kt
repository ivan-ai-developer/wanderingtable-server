package ru.gohasoft.wanderingtable.database.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.gohasoft.wanderingtable.database.model.event.GameEvent

interface GameEventRepository : JpaRepository<GameEvent, String> {

    /**
     * История партий игрока: и клубные, и турнирные, поскольку оба типа — наследники
     * `GameEvent` и лежат в одной таблице `game_events`.
     */
    @Query(
        """
        select ge from GameEvent ge
         where ge.id in (
            select p.eventId from EventParticipant p where p.userId = :userId
         )
        """
    )
    fun findByParticipant(@Param("userId") userId: String, pageable: Pageable): Page<GameEvent>
}
