package ru.gohasoft.wanderingtable.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.gohasoft.wanderingtable.database.model.event.LeagueStanding

interface LeagueStandingRepository : JpaRepository<LeagueStanding, String> {

    fun findByLeagueIdAndUserId(leagueId: String, userId: String): LeagueStanding?

    fun findByLeagueIdOrderByPointsDesc(leagueId: String): List<LeagueStanding>

    /**
     * Идемпотентно создаёт строку таблицы лиги.
     *
     * `ON CONFLICT DO NOTHING` вместо «прочитать → вставить»: последний вариант при
     * одновременном начислении двум игрокам приводил бы к нарушению уникального индекса,
     * а перехватить его и продолжить в той же транзакции PostgreSQL уже не позволяет —
     * транзакция становится аварийной.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        value = """
            insert into league_standings (id, league_id, user_id, points)
            values (:id, :leagueId, :userId, 0)
            on conflict (league_id, user_id) do nothing
        """,
        nativeQuery = true
    )
    fun ensureExists(
        @Param("id") id: String,
        @Param("leagueId") leagueId: String,
        @Param("userId") userId: String
    ): Int

    /**
     * Атомарное начисление очков.
     *
     * Именно инкремент в SQL, а не «прочитать points → прибавить → сохранить»: при
     * одновременном завершении двух партий лиги последний вариант терял одно начисление.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        value = """
            update league_standings
               set points = points + :delta
             where league_id = :leagueId
               and user_id = :userId
        """,
        nativeQuery = true
    )
    fun incrementPoints(
        @Param("leagueId") leagueId: String,
        @Param("userId") userId: String,
        @Param("delta") delta: Int
    ): Int
}
