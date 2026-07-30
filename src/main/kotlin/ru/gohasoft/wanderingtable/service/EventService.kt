package ru.gohasoft.wanderingtable.service

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.database.model.Role
import ru.gohasoft.wanderingtable.database.model.event.Event
import ru.gohasoft.wanderingtable.database.model.event.EventParticipant
import ru.gohasoft.wanderingtable.database.model.event.EventStatus
import ru.gohasoft.wanderingtable.database.repository.EventParticipantRepository
import ru.gohasoft.wanderingtable.database.repository.EventRepository
import kotlin.jvm.optionals.getOrNull

/**
 * Жизненный цикл событий и состав участников — общее для партий и турнирных событий.
 */
@Service
class EventService(
    private val eventRepository: EventRepository,
    private val eventParticipantRepository: EventParticipantRepository,
    private val userService: UserService
) {

    fun requireById(eventId: ObjectId): Event =
        eventRepository.findById(eventId.value).getOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found.")

    fun participantIds(eventId: ObjectId): List<String> =
        eventParticipantRepository.findUserIdsByEventId(eventId.value)

    fun list(status: EventStatus?, gameId: String?, pageable: Pageable): Page<Event> = when {
        status != null && gameId != null ->
            eventRepository.findByStatusAndGameId(status, gameId, pageable)
        status != null -> eventRepository.findByStatus(status, pageable)
        gameId != null -> eventRepository.findByGameId(gameId, pageable)
        else -> eventRepository.findAll(pageable)
    }

    /**
     * Вступление в событие.
     *
     * Порядок шагов важен: проверка «уже участник» идёт до занятия места, чтобы повторный
     * запрос не расходовал слот; уникальный индекс `uk_event_participants_event_user`
     * остаётся последним рубежом на случай гонки двух одновременных вступлений.
     */
    @PreAuthorize("hasRole('PLAYER')")
    @Transactional
    fun join(userId: ObjectId, eventId: ObjectId): Event {
        val event = requireById(eventId)
        if (event.status != EventStatus.PLANNED) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Event is not accepting participants (status=${event.status})."
            )
        }
        if (eventParticipantRepository.existsByEventIdAndUserId(eventId.value, userId.value)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Already joined this event.")
        }

        if (eventRepository.tryReserveSeat(eventId.value, EventStatus.PLANNED.name) == 0) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "No seats left or the event is no longer accepting participants."
            )
        }

        try {
            eventParticipantRepository.saveAndFlush(
                EventParticipant(eventId = eventId, userId = userId)
            )
        } catch (e: DataIntegrityViolationException) {
            // Гонка двойного вступления: занятое место освобождается откатом транзакции.
            throw ResponseStatusException(HttpStatus.CONFLICT, "Already joined this event.")
        }

        return requireById(eventId)
    }

    @Transactional
    fun leave(userId: ObjectId, eventId: ObjectId) {
        val event = requireById(eventId)
        if (event.status != EventStatus.PLANNED) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Cannot leave an event that has already started."
            )
        }
        if (event.creatorId == userId) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "The creator cannot leave their own event; cancel it instead."
            )
        }
        if (eventParticipantRepository.removeParticipant(eventId.value, userId.value) == 0) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Not a participant of this event.")
        }
        eventRepository.releaseSeat(eventId.value)
    }

    @Transactional
    fun start(actorId: ObjectId, eventId: ObjectId): Event {
        val event = requireById(eventId)
        requireCreatorOrClubManager(actorId, event)
        if (event.participantsCount < event.minParticipants) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Not enough participants: ${event.participantsCount} of ${event.minParticipants}."
            )
        }
        if (eventRepository.tryTransition(
                eventId.value,
                listOf(EventStatus.PLANNED.name),
                EventStatus.IN_PROGRESS.name
            ) == 0
        ) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Event is not in PLANNED state.")
        }
        return requireById(eventId)
    }

    @Transactional
    fun cancel(actorId: ObjectId, eventId: ObjectId): Event {
        val event = requireById(eventId)
        requireCreatorOrClubManager(actorId, event)
        if (eventRepository.tryTransition(
                eventId.value,
                listOf(EventStatus.PLANNED.name, EventStatus.IN_PROGRESS.name),
                EventStatus.CANCELLED.name
            ) == 0
        ) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Event cannot be cancelled (status=${event.status})."
            )
        }
        return requireById(eventId)
    }

    fun requireCreatorOrClubManager(actorId: ObjectId, event: Event) {
        if (event.creatorId == actorId) return
        if (userService.requireById(actorId).hasRole(Role.CLUB_MANAGER)) return
        throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to manage this event.")
    }
}
