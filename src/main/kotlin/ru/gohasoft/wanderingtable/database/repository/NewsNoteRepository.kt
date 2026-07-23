package ru.gohasoft.wanderingtable.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.gohasoft.wanderingtable.database.model.NewsNote
import ru.gohasoft.wanderingtable.database.model.ObjectId

interface NewsNoteRepository : JpaRepository<NewsNote, ObjectId> {
    fun findByOwnerId(ownerId: ObjectId): List<NewsNote>
}