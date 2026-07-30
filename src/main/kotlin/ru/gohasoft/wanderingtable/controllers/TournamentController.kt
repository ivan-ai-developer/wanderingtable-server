package ru.gohasoft.wanderingtable.controllers

import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.gohasoft.wanderingtable.controllers.dto.ChampionshipStandingResponse
import ru.gohasoft.wanderingtable.controllers.dto.CreateLeagueGameRequest
import ru.gohasoft.wanderingtable.controllers.dto.CreateTournamentRequest
import ru.gohasoft.wanderingtable.controllers.dto.EventResponse
import ru.gohasoft.wanderingtable.controllers.dto.LeagueStandingResponse
import ru.gohasoft.wanderingtable.controllers.dto.PageResponse
import ru.gohasoft.wanderingtable.controllers.dto.TournamentResponse
import ru.gohasoft.wanderingtable.controllers.dto.toResponse
import ru.gohasoft.wanderingtable.controllers.dto.toTournamentResponse
import ru.gohasoft.wanderingtable.controllers.utils.getCurrentObjectId
import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.database.model.event.EventStatus
import ru.gohasoft.wanderingtable.service.EventService
import ru.gohasoft.wanderingtable.service.TournamentService

/**
 * Турнирные события. Вступление, старт и отмена выполняются общими эндпоинтами
 * `/events/{id}/...`, поскольку турнир — такой же наследник `Event`, как и партия.
 */
@RestController
@RequestMapping("/events/tournaments")
class TournamentController(
    private val tournamentService: TournamentService,
    private val eventService: EventService
) {

    @PostMapping
    fun create(@Valid @RequestBody body: CreateTournamentRequest): TournamentResponse =
        tournamentService.create(
            creatorId = getCurrentObjectId(),
            kind = body.kind,
            gameId = ObjectId(body.gameId),
            title = body.title,
            description = body.description,
            startsAt = body.startsAt,
            endsAt = body.endsAt,
            entryFee = body.entryFee,
            expectedSkillLevel = body.expectedSkillLevel,
            minParticipants = body.minParticipants,
            maxParticipants = body.maxParticipants,
            bracketStrategy = body.bracketStrategy,
            eliminationStrategy = body.eliminationStrategy,
            scoringStrategy = body.scoringStrategy,
            seasonStart = body.seasonStart,
            seasonEnd = body.seasonEnd
        ).toTournamentResponse()

    @GetMapping
    fun list(
        @RequestParam(required = false) status: EventStatus?,
        @RequestParam(required = false) gameId: String?,
        @PageableDefault(size = 20, sort = ["startsAt"], direction = Sort.Direction.ASC)
        pageable: Pageable
    ): PageResponse<TournamentResponse> =
        tournamentService.list(status, gameId?.takeIf { it.isNotBlank() }, pageable)
            .toResponse { it.toTournamentResponse() }

    @GetMapping("/{id}")
    fun findById(@PathVariable id: String): TournamentResponse {
        val tournamentId = ObjectId(id)
        return tournamentService.requireById(tournamentId)
            .toTournamentResponse(participants = eventService.participantIds(tournamentId))
    }

    /** Порождает сетку партий по стратегии турнира. */
    @PostMapping("/{id}/bracket")
    fun generateBracket(@PathVariable id: String): List<EventResponse> =
        tournamentService.generateBracket(getCurrentObjectId(), ObjectId(id))
            .map { it.toResponse() }

    @GetMapping("/{id}/games")
    fun games(@PathVariable id: String): List<EventResponse> =
        tournamentService.gamesOf(ObjectId(id)).map { it.toResponse() }

    /**
     * Заводит партию лиги произвольным составом её участников — у лиги нет фиксированной
     * сетки, поэтому вместо `/bracket` используется этот эндпоинт.
     */
    @PostMapping("/{id}/games")
    fun createLeagueGame(
        @PathVariable id: String,
        @Valid @RequestBody body: CreateLeagueGameRequest
    ): EventResponse =
        tournamentService.createLeagueGame(
            actorId = getCurrentObjectId(),
            leagueId = ObjectId(id),
            participantIds = body.participantIds,
            startsAt = body.startsAt,
            durationMinutes = body.durationMinutes
        ).toResponse()

    @GetMapping("/{id}/standings")
    fun leagueStandings(@PathVariable id: String): List<LeagueStandingResponse> =
        tournamentService.leagueStandings(ObjectId(id)).map { it.toResponse() }

    @GetMapping("/{id}/elimination")
    fun championshipStandings(@PathVariable id: String): List<ChampionshipStandingResponse> =
        tournamentService.championshipStandings(ObjectId(id)).map { it.toResponse() }
}
