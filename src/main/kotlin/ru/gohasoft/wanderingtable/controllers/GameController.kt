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
import ru.gohasoft.wanderingtable.controllers.dto.CreateGameRequest
import ru.gohasoft.wanderingtable.controllers.dto.GameResponse
import ru.gohasoft.wanderingtable.controllers.dto.PageResponse
import ru.gohasoft.wanderingtable.controllers.dto.toResponse
import ru.gohasoft.wanderingtable.controllers.utils.getCurrentObjectId
import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.service.GameService

@RestController
@RequestMapping("/games")
class GameController(
    private val gameService: GameService
) {

    @PostMapping
    fun create(@Valid @RequestBody body: CreateGameRequest): GameResponse =
        gameService.create(
            creatorId = getCurrentObjectId(),
            name = body.name,
            description = body.description,
            minPlayers = body.minPlayers,
            maxPlayers = body.maxPlayers,
            resultType = body.resultType
        ).toResponse()

    @GetMapping
    fun findAll(
        @RequestParam(required = false) name: String?,
        @PageableDefault(size = 20, sort = ["name"], direction = Sort.Direction.ASC)
        pageable: Pageable
    ): PageResponse<GameResponse> {
        val page = if (name.isNullOrBlank()) {
            gameService.findAll(pageable)
        } else {
            gameService.search(name, pageable)
        }
        return page.toResponse { it.toResponse() }
    }

    @GetMapping("/{id}")
    fun findById(@PathVariable id: String): GameResponse =
        gameService.requireById(ObjectId(id)).toResponse()
}
