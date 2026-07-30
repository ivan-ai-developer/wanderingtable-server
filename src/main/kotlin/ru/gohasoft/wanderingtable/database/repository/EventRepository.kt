package ru.gohasoft.wanderingtable.database.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.gohasoft.wanderingtable.database.model.event.Event
import ru.gohasoft.wanderingtable.database.model.event.EventStatus

/**
 * Изменения состояния события выполняются **нативным SQL**, а не JPQL.
 *
 * Это не микрооптимизация, а требование корректности. `Event` — корень JOINED-иерархии,
 * и JPQL-bulk-update по нему Hibernate реализует в две фазы через временную таблицу:
 * сначала `select` подходящих id, затем `update ... where id in (...)`. Предикат при этом
 * вычисляется незаблокированным чтением, из-за чего условие
 * `participants_count < max_participants` перестаёт защищать от гонки — в тесте все 12
 * одновременных вступлений проходили в событие на 5 мест.
 *
 * Нативный `UPDATE` по таблице `events` — один оператор: PostgreSQL берёт блокировку строки
 * и вычисляет условие уже на актуальном значении, поэтому из конкурентных запросов
 * проходит ровно столько, сколько есть свободных мест.
 */
interface EventRepository : JpaRepository<Event, String> {

    /** Атомарно занимает место: 1 — успех, 0 — мест нет или событие не набирает участников. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        value = """
            update events
               set participants_count = participants_count + 1,
                   version = version + 1
             where id = :eventId
               and status = :requiredStatus
               and participants_count < max_participants
        """,
        nativeQuery = true
    )
    fun tryReserveSeat(
        @Param("eventId") eventId: String,
        @Param("requiredStatus") requiredStatus: String
    ): Int

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        value = """
            update events
               set participants_count = participants_count - 1,
                   version = version + 1
             where id = :eventId
               and participants_count > 0
        """,
        nativeQuery = true
    )
    fun releaseSeat(@Param("eventId") eventId: String): Int

    /**
     * Атомарный переход статуса; 0 означает, что событие уже вышло из состояний [from]
     * — например, партию одновременно завершили или отменили дважды.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        value = """
            update events
               set status = :to,
                   version = version + 1
             where id = :eventId
               and status in (:from)
        """,
        nativeQuery = true
    )
    fun tryTransition(
        @Param("eventId") eventId: String,
        @Param("from") from: Collection<String>,
        @Param("to") to: String
    ): Int

    fun findByStatus(status: EventStatus, pageable: Pageable): Page<Event>

    fun findByGameId(gameId: String, pageable: Pageable): Page<Event>

    fun findByStatusAndGameId(status: EventStatus, gameId: String, pageable: Pageable): Page<Event>
}
