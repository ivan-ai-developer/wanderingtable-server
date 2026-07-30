package ru.gohasoft.wanderingtable.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.gohasoft.wanderingtable.database.model.Role
import ru.gohasoft.wanderingtable.database.model.User

/**
 * Идентификатор в сигнатурах репозиториев — `String`, а не `ObjectId`.
 *
 * Причина: `ObjectId` объявлен как `@JvmInline value class`, и Kotlin искажает (mangles)
 * JVM-имена методов, принимающих value class в параметрах — Spring Data не смогла бы
 * вывести запрос по такому имени, а тип идентификатора в метамодели JPA равен `String`.
 * В самих сущностях `ObjectId` сохраняется: там поле стирается до `String`,
 * и Hibernate видит обычный varchar-столбец.
 */
interface UserRepository : JpaRepository<User, String> {

    fun findByEmail(email: String): User?

    @Query("select count(u) from User u join u.roles r where r = :role")
    fun countByRole(@Param("role") role: Role): Long
}
