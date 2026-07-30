package ru.gohasoft.wanderingtable.database.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.gohasoft.wanderingtable.database.model.result.GameResult
import ru.gohasoft.wanderingtable.database.model.result.Outcome

/** Число партий по конкретной игре справочника — строка для «топ-5 любимых игр». */
interface GamePlayCount {
    val gameId: String
    val played: Long
}

interface GameResultRepository : JpaRepository<GameResult, String> {

    fun findByGameEventId(gameEventId: String): List<GameResult>

    fun existsByGameEventId(gameEventId: String): Boolean

    /**
     * Все запросы статистики опираются только на базовую таблицу `game_results`
     * (поля `user_id` и `outcome`), поэтому не требуют JOIN с подтаблицами результатов
     * и не меняются при добавлении нового типа счёта.
     */
    @Query(
        """
        select count(r) from GameResult r
         where r.userId = :userId
           and r.outcome <> ru.gohasoft.wanderingtable.database.model.result.Outcome.UNDEFINED
        """
    )
    fun countPlayed(@Param("userId") userId: String): Long

    @Query("select count(r) from GameResult r where r.userId = :userId and r.outcome = :outcome")
    fun countByOutcome(
        @Param("userId") userId: String,
        @Param("outcome") outcome: Outcome
    ): Long

    /** Топ игр по числу сыгранных партий; учитывает и клубные, и турнирные партии. */
    @Query(
        """
        select ge.gameId as gameId, count(r) as played
          from GameResult r
          join GameEvent ge on ge.id = r.gameEventId
         where r.userId = :userId
         group by ge.gameId
         order by count(r) desc, ge.gameId asc
        """
    )
    fun findTopGames(@Param("userId") userId: String, pageable: Pageable): List<GamePlayCount>
}
