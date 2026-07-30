package ru.gohasoft.wanderingtable.controllers

import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.gohasoft.wanderingtable.controllers.dto.CreateRegularGameRequest
import ru.gohasoft.wanderingtable.controllers.dto.EventResponse
import ru.gohasoft.wanderingtable.controllers.dto.FinishGameEventRequest
import ru.gohasoft.wanderingtable.controllers.dto.FinishedGameEventResponse
import ru.gohasoft.wanderingtable.controllers.dto.GameResultResponse
import ru.gohasoft.wanderingtable.controllers.dto.PageResponse
import ru.gohasoft.wanderingtable.controllers.dto.toResponse
import ru.gohasoft.wanderingtable.controllers.utils.getCurrentObjectId
import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.database.model.event.EventStatus
import ru.gohasoft.wanderingtable.service.EventService
import ru.gohasoft.wanderingtable.service.GameEventService

@RestController
@RequestMapping("/events")
class EventController(
    private val eventService: EventService,
    private val gameEventService: GameEventService
) {

    @PostMapping("/regular-games")
    fun createRegularGame(@Valid @RequestBody body: CreateRegularGameRequest): EventResponse =
        gameEventService.createRegularGame(
            creatorId = getCurrentObjectId(),
            gameId = ObjectId(body.gameId),
            title = body.title,
            description = body.description,
            startsAt = body.startsAt,
            durationMinutes = body.durationMinutes,
            minParticipants = body.minParticipants,
            maxParticipants = body.maxParticipants
        ).toResponse()

    /** Общее расписание клуба: партии и турнирные события вместе. */
    @GetMapping
    fun list(
        @RequestParam(required = false) status: EventStatus?,
        @RequestParam(required = false) gameId: String?,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable
    ): PageResponse<EventResponse> =
        eventService.list(status, gameId?.takeIf { it.isNotBlank() }, pageable)
            .toResponse { it.toResponse() }

    @GetMapping("/{id}")
    fun findById(@PathVariable id: String): EventResponse {
        val eventId = ObjectId(id)
        return eventService.requireById(eventId)
            .toResponse(participants = eventService.participantIds(eventId))
    }

    @PostMapping("/{id}/join")
    fun join(@PathVariable id: String): EventResponse =
        eventService.join(getCurrentObjectId(), ObjectId(id)).toResponse()

    @DeleteMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun leave(@PathVariable id: String) {
        eventService.leave(getCurrentObjectId(), ObjectId(id))
    }

    @PostMapping("/{id}/start")
    fun start(@PathVariable id: String): EventResponse =
        eventService.start(getCurrentObjectId(), ObjectId(id)).toResponse()

    /** Завершение партии с подведением итогов: исход каждого игрока вычисляет сервер. */
    @PostMapping("/{id}/finish")
    fun finish(
        @PathVariable id: String,
        @Valid @RequestBody body: FinishGameEventRequest
    ): FinishedGameEventResponse {
        val (event, results) = gameEventService.finish(
            actorId = getCurrentObjectId(),
            eventId = ObjectId(id),
            submitted = body.results
        )
        return FinishedGameEventResponse(
            event = event.toResponse(),
            results = results.map { it.toResponse() }
        )
    }

    @GetMapping("/{id}/results")
    fun results(@PathVariable id: String): List<GameResultResponse> =
        gameEventService.resultsOf(ObjectId(id)).map { it.toResponse() }

    /** Отмена события: статус переводится в CANCELLED, история не удаляется. */
    @DeleteMapping("/{id}")
    fun cancel(@PathVariable id: String): EventResponse =
        eventService.cancel(getCurrentObjectId(), ObjectId(id)).toResponse()
}
