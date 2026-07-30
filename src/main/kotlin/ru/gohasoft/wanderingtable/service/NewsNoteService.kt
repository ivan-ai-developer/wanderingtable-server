package ru.gohasoft.wanderingtable.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.gohasoft.wanderingtable.database.model.NewsNote
import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.database.model.Role
import ru.gohasoft.wanderingtable.database.model.nowTruncated
import ru.gohasoft.wanderingtable.database.repository.NewsNoteRepository
import kotlin.jvm.optionals.getOrNull

@Service
class NewsNoteService(
    private val newsNoteRepository: NewsNoteRepository,
    private val userService: UserService
) {

    /** Лента новостей клуба — читается без авторизации. */
    fun findAll(pageable: Pageable): Page<NewsNote> = newsNoteRepository.findAll(pageable)

    fun findByOwner(ownerId: ObjectId, pageable: Pageable): Page<NewsNote> =
        newsNoteRepository.findByOwnerId(ownerId.value, pageable)

    @PreAuthorize("hasRole('NEWS_CREATOR')")
    @Transactional
    fun save(ownerId: ObjectId, id: String?, title: String, content: String): NewsNote {
        val existing = id?.let { newsNoteRepository.findById(it).getOrNull() }
        if (existing != null && existing.ownerId != ownerId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to modify this note.")
        }
        val note = existing?.copy(title = title, content = content)
            ?: NewsNote(
                id = id?.let(::ObjectId) ?: ObjectId.get(),
                title = title,
                content = content,
                createdAt = nowTruncated(),
                ownerId = ownerId
            )
        return newsNoteRepository.save(note)
    }

    @Transactional
    fun deleteById(actorId: ObjectId, noteId: ObjectId) {
        val note = newsNoteRepository.findById(noteId.value).getOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found.")

        // Раньше удаление чужой записи молча возвращало 200, ничего не удалив.
        val isOwner = note.ownerId == actorId
        val isClubManager = userService.requireById(actorId).hasRole(Role.CLUB_MANAGER)
        if (!isOwner && !isClubManager) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to delete this note.")
        }
        newsNoteRepository.deleteById(note.id.value)
    }
}
