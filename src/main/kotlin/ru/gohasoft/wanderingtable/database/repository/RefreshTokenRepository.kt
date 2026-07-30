package ru.gohasoft.wanderingtable.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.gohasoft.wanderingtable.database.model.RefreshToken
import java.time.Instant

interface RefreshTokenRepository : JpaRepository<RefreshToken, String> {

    /**
     * Атомарно расходует токен, возвращая количество удалённых строк.
     *
     * `0` означает, что токен уже израсходован конкурентным запросом. Это именно bulk-delete
     * через [Query], а не производный `deleteBy...`: производный вариант сначала загружает
     * сущность, затем удаляет её и возвращает число *загруженных* строк — при гонке оба
     * запроса получили бы по единице и оба выдали бы новую пару токенов.
     */
    @Modifying
    @Query("delete from RefreshToken t where t.userId = :userId and t.hashedToken = :hashedToken")
    fun consume(@Param("userId") userId: String, @Param("hashedToken") hashedToken: String): Int

    @Modifying
    @Query("delete from RefreshToken t where t.userId = :userId")
    fun deleteAllForUser(@Param("userId") userId: String): Int

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :now")
    fun deleteExpired(@Param("now") now: Instant): Int
}
