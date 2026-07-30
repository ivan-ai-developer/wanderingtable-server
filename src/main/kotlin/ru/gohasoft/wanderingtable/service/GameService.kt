package ru.gohasoft.wanderingtable.service

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.database.model.game.Game
import ru.gohasoft.wanderingtable.database.model.game.ResultType
import ru.gohasoft.wanderingtable.database.repository.GameRepository
import kotlin.jvm.optionals.getOrNull

@Service
class GameService(
    private val gameRepository: GameRepository
) {

    fun requireById(gameId: ObjectId): Game =
        gameRepository.findById(gameId.value).getOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found.")

    fun findAll(pageable: Pageable): Page<Game> = gameRepository.findAll(pageable)

    fun search(name: String, pageable: Pageable): Page<Game> =
        gameRepository.findByNameContainingIgnoreCase(name, pageable)

    @PreAuthorize("hasRole('GAME_CREATOR')")
    @Transactional
    fun create(
        creatorId: ObjectId,
        name: String,
        description: String,
        minPlayers: Int,
        maxPlayers: Int,
        resultType: ResultType
    ): Game {
        if (maxPlayers < minPlayers) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "maxPlayers must be greater than or equal to minPlayers."
            )
        }
        return try {
            gameRepository.save(
                Game(
                    name = name.trim(),
                    description = description.trim(),
                    minPlayers = minPlayers,
                    maxPlayers = maxPlayers,
                    resultType = resultType,
                    creatorId = creatorId
                )
            )
        } catch (e: DataIntegrityViolationException) {
            // Уникальность имени обеспечивает индекс uk_games_name, а не проверка чтением:
            // иначе два одновременных запроса создали бы две «Каркассон» и статистика
            // по игре разъехалась бы на две записи справочника.
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "A game with that name already exists."
            )
        }
    }
}
