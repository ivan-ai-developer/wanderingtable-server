package ru.gohasoft.wanderingtable.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.gohasoft.wanderingtable.database.model.event.EventParticipant

interface EventParticipantRepository : JpaRepository<EventParticipant, String> {

    fun existsByEventIdAndUserId(eventId: String, userId: String): Boolean

    fun findByEventId(eventId: String): List<EventParticipant>

    @Query("select p.userId from EventParticipant p where p.eventId = :eventId order by p.joinedAt")
    fun findUserIdsByEventId(@Param("eventId") eventId: String): List<String>

    /** Возвращает число удалённых строк: 0 значит, что пользователь и не был участником. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from EventParticipant p where p.eventId = :eventId and p.userId = :userId")
    fun removeParticipant(
        @Param("eventId") eventId: String,
        @Param("userId") userId: String
    ): Int
}
