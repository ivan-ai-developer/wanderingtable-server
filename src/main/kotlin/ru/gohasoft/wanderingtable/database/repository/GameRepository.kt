package ru.gohasoft.wanderingtable.database.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import ru.gohasoft.wanderingtable.database.model.game.Game

interface GameRepository : JpaRepository<Game, String> {

    fun findByNameIgnoreCase(name: String): Game?

    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<Game>
}
