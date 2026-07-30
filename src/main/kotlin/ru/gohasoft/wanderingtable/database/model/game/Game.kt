package ru.gohasoft.wanderingtable.database.model.game

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.database.model.nowTruncated
import java.time.Instant

/**
 * Настольная игра из справочника клуба («Каркассон», «Манчкин»).
 *
 * Это не партия: конкретные сыгранные партии — это `RegularGame` и `TournamentGame`,
 * наследники `GameEvent`, каждый из которых ссылается на запись справочника.
 */
@Entity
@Table(
    name = "games",
    uniqueConstraints = [UniqueConstraint(name = "uk_games_name", columnNames = ["name"])]
)
data class Game(
    @Id val id: ObjectId = ObjectId.get(),
    var name: String,
    var description: String = "",
    @Column(name = "min_players") var minPlayers: Int,
    @Column(name = "max_players") var maxPlayers: Int,
    @Enumerated(EnumType.STRING)
    @Column(name = "result_type")
    val resultType: ResultType,
    val creatorId: ObjectId,
    val createdAt: Instant = nowTruncated()
)
