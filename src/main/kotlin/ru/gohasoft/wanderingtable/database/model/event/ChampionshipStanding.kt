package ru.gohasoft.wanderingtable.database.model.event

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import ru.gohasoft.wanderingtable.database.model.ObjectId

/**
 * Состояние участника чемпионата. `eliminatedAtRound == null` означает, что игрок ещё в игре.
 */
@Entity
@Table(
    name = "championship_standings",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_championship_standings_championship_user",
            columnNames = ["championship_id", "user_id"]
        )
    ]
)
data class ChampionshipStanding(
    @Id val id: ObjectId = ObjectId.get(),
    @Column(name = "championship_id", nullable = false) val championshipId: ObjectId,
    @Column(name = "user_id", nullable = false) val userId: ObjectId,
    @Column(name = "eliminated_at_round") var eliminatedAtRound: Int? = null
)
