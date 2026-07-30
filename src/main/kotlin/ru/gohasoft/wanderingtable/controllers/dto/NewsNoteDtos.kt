package ru.gohasoft.wanderingtable.controllers.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import ru.gohasoft.wanderingtable.database.model.NewsNote
import java.time.Instant

data class NewsNoteRequest(
    /** Если задан и запись существует — обновление, иначе создание. */
    val id: String?,
    @field:NotBlank(message = "Title can't be blank.")
    @field:Size(max = 200, message = "Title must be at most 200 characters long.")
    val title: String,
    val content: String
)

data class NewsNoteResponse(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Instant,
    val ownerId: String
)

fun NewsNote.toResponse() = NewsNoteResponse(
    id = id.value,
    title = title,
    content = content,
    createdAt = createdAt,
    ownerId = ownerId.value
)
