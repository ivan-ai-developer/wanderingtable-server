package ru.gohasoft.wanderingtable.controllers.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import ru.gohasoft.wanderingtable.database.model.event.Event
import ru.gohasoft.wanderingtable.database.model.event.EventStatus
import ru.gohasoft.wanderingtable.database.model.event.GameEvent
import java.time.Instant

data class CreateRegularGameRequest(
    @field:NotBlank(message = "gameId can't be blank.")
    val gameId: String,
    @field:NotBlank(message = "Title can't be blank.")
    @field:Size(max = 200, message = "Title must be at most 200 characters long.")
    val title: String,
    @field:Size(max = 2000, message = "Description must be at most 2000 characters long.")
    val description: String = "",
    val startsAt: Instant,
    @field:Min(value = 1, message = "durationMinutes must be at least 1.")
    val durationMinutes: Int? = null,
    @field:Min(value = 1, message = "minParticipants must be at least 1.")
    val minParticipants: Int,
    @field:Min(value = 1, message = "maxParticipants must be at least 1.")
    val maxParticipants: Int
)

/**
 * Единый ответ для всех типов событий с дискриминатором [type].
 *
 * Поля, специфичные для подтипов, нулевые для остальных. Плоская структура выбрана
 * сознательно: полиморфный JSON потребовал бы от Android-клиента иерархии моделей,
 * а перечень подтипов ещё будет расширяться.
 */
data class EventResponse(
    val id: String,
    val type: String,
    val title: String,
    val description: String,
    val gameId: String,
    val creatorId: String,
    val status: EventStatus,
    val minParticipants: Int,
    val maxParticipants: Int,
    val participantsCount: Int,
    val createdAt: Instant,
    /** Только для партий (`GameEvent`). */
    val startsAt: Instant? = null,
    /** Только для партий (`GameEvent`). */
    val durationMinutes: Int? = null,
    /** Заполняется только при запросе одного события. */
    val participants: List<String>? = null
)

fun Event.toResponse(participants: List<String>? = null) = EventResponse(
    id = id.value,
    type = eventType,
    title = title,
    description = description,
    gameId = gameId.value,
    creatorId = creatorId.value,
    status = status,
    minParticipants = minParticipants,
    maxParticipants = maxParticipants,
    participantsCount = participantsCount,
    createdAt = createdAt,
    startsAt = (this as? GameEvent)?.startsAt,
    durationMinutes = (this as? GameEvent)?.durationMinutes,
    participants = participants
)
