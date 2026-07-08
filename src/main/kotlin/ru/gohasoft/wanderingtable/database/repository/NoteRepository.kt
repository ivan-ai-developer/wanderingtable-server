package ru.gohasoft.wanderingtable.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.gohasoft.wanderingtable.database.model.Note
import ru.gohasoft.wanderingtable.database.model.ObjectId

interface NoteRepository: JpaRepository<Note, ObjectId> {
    fun findByOwnerId(ownerId: ObjectId): List<Note>
}