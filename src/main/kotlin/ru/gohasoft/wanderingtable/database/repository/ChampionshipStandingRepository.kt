package ru.gohasoft.wanderingtable.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.gohasoft.wanderingtable.database.model.event.ChampionshipStanding

interface ChampionshipStandingRepository : JpaRepository<ChampionshipStanding, String> {

    fun findByChampionshipId(championshipId: String): List<ChampionshipStanding>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        value = """
            insert into championship_standings (id, championship_id, user_id, eliminated_at_round)
            values (:id, :championshipId, :userId, null)
            on conflict (championship_id, user_id) do nothing
        """,
        nativeQuery = true
    )
    fun ensureExists(
        @Param("id") id: String,
        @Param("championshipId") championshipId: String,
        @Param("userId") userId: String
    ): Int

    /**
     * Помечает выбывание только для тех, кто ещё в игре: условие `is null` делает операцию
     * идемпотентной, поэтому повторное подведение итогов раунда не переписывает раунд выбывания.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        value = """
            update championship_standings
               set eliminated_at_round = :round
             where championship_id = :championshipId
               and user_id = :userId
               and eliminated_at_round is null
        """,
        nativeQuery = true
    )
    fun markEliminated(
        @Param("championshipId") championshipId: String,
        @Param("userId") userId: String,
        @Param("round") round: Int
    ): Int
}
