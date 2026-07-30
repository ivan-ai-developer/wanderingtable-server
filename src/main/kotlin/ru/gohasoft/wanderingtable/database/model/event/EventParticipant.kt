package ru.gohasoft.wanderingtable.database.model.event

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.database.model.nowTruncated
import java.time.Instant

/**
 * Участие пользователя в событии — единый источник правды по составу участников.
 *
 * Работает и для партий, и для турнирных событий, поскольку оба наследуют [Event].
 * Уникальный индекс по паре (событие, пользователь) — то, что реально отсекает двойное
 * вступление при двойном клике или параллельных запросах.
 */
@Entity
@Table(
    name = "event_participants",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_event_participants_event_user",
            columnNames = ["event_id", "user_id"]
        )
    ],
    indexes = [
        Index(name = "idx_event_participants_user_id", columnList = "user_id"),
        Index(name = "idx_event_participants_event_id", columnList = "event_id")
    ]
)
data class EventParticipant(
    @Id val id: ObjectId = ObjectId.get(),
    @Column(name = "event_id", nullable = false) val eventId: ObjectId,
    @Column(name = "user_id", nullable = false) val userId: ObjectId,
    val joinedAt: Instant = nowTruncated()
)
