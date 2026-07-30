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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.gohasoft.wanderingtable.controllers.dto.NewsNoteRequest
import ru.gohasoft.wanderingtable.controllers.dto.NewsNoteResponse
import ru.gohasoft.wanderingtable.controllers.dto.PageResponse
import ru.gohasoft.wanderingtable.controllers.dto.toResponse
import ru.gohasoft.wanderingtable.controllers.utils.getCurrentObjectId
import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.service.NewsNoteService

@RestController
@RequestMapping("/notes")
class NewsNoteController(
    private val newsNoteService: NewsNoteService
) {

    /** Лента новостей клуба. Публично: новости читают и незарегистрированные посетители. */
    @GetMapping
    fun findAll(
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable
    ): PageResponse<NewsNoteResponse> =
        newsNoteService.findAll(pageable).toResponse { it.toResponse() }

    /** Новости, созданные текущим пользователем. */
    @GetMapping("/my")
    fun findMine(
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable
    ): PageResponse<NewsNoteResponse> =
        newsNoteService.findByOwner(getCurrentObjectId(), pageable).toResponse { it.toResponse() }

    @PostMapping
    fun save(@Valid @RequestBody body: NewsNoteRequest): NewsNoteResponse =
        newsNoteService.save(
            ownerId = getCurrentObjectId(),
            id = body.id,
            title = body.title,
            content = body.content
        ).toResponse()

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteById(@PathVariable id: String) {
        newsNoteService.deleteById(getCurrentObjectId(), ObjectId(id))
    }
}
