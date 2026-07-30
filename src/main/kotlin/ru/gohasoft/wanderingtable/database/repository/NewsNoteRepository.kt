package ru.gohasoft.wanderingtable.database.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import ru.gohasoft.wanderingtable.database.model.NewsNote

interface NewsNoteRepository : JpaRepository<NewsNote, String> {

    fun findByOwnerId(@Param("ownerId") ownerId: String, pageable: Pageable): Page<NewsNote>
}
