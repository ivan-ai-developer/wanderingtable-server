package ru.gohasoft.wanderingtable.controllers

import ru.gohasoft.wanderingtable.controllers.NewsNoteController.NewsNoteResponse
import ru.gohasoft.wanderingtable.database.model.NewsNote
import ru.gohasoft.wanderingtable.database.repository.NewsNoteRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import ru.gohasoft.wanderingtable.controllers.utils.getCurrentObjectId
import ru.gohasoft.wanderingtable.database.model.ObjectId
import java.time.Instant
import kotlin.jvm.optionals.getOrNull

// POST http://localhost:8085/notes
// GET http://localhost:8085/notes?ownerId=123
// DELETE http://localhost:8085/notes/123

@RestController
@RequestMapping("/notes")
class NewsNoteController(
    private val newsNoteRepository: NewsNoteRepository
) {

    data class NewsNoteRequest(
        val id: String?,
        @field:NotBlank(message = "Title can't be blank.")
        val title: String,
        val content: String,
    )

    data class NewsNoteResponse(
        val id: String,
        val title: String,
        val content: String,
        val createdAt: Instant
    )

    @PostMapping
    fun save(
        @Valid @RequestBody body: NewsNoteRequest
    ): NewsNoteResponse {
        val ownerId = getCurrentObjectId(ID_ERROR_MESSAGE)
        val newsNoteId = body.id?.let(::ObjectId)
        val oldNewsNote = newsNoteRepository.findById(newsNoteId).getOrNull()
        if (oldNewsNote != null && oldNewsNote.ownerId != ownerId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to modify this note.")
        }
        val newsNote = newsNoteRepository.save(
            oldNewsNote?.copy(
                title = body.title,
                content = body.content
            ) ?: NewsNote(
                id = newsNoteId ?: ObjectId.get(),
                title = body.title,
                content = body.content,
                createdAt = Instant.now(),
                ownerId = ownerId
            )
        )
        return newsNote.toResponse()
    }

    @GetMapping
    fun findByOwnerId(): List<NewsNoteResponse> {
        val ownerId = getCurrentObjectId(ID_ERROR_MESSAGE)
        return newsNoteRepository.findByOwnerId(ownerId).map {
            it.toResponse()
        }
    }

    @DeleteMapping(path = ["/{id}"])
    fun deleteById(@PathVariable id: String) {
        val note = newsNoteRepository.findById(ObjectId(id)).orElseThrow {
            IllegalArgumentException("Note not found")
        }
        val ownerId = getCurrentObjectId(ID_ERROR_MESSAGE)
        if(note.ownerId == ownerId) {
            newsNoteRepository.deleteById(ObjectId(id))
        }
    }

    companion object {
        private const val ID_ERROR_MESSAGE = "Note ownerId not right value"
    }
}

private fun NewsNote.toResponse(): NewsNoteResponse {
    return NewsNoteResponse(
        id = id.value,
        title = title,
        content = content,
        createdAt = createdAt
    )
}