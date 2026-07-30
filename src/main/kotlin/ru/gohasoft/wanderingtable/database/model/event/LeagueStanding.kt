package ru.gohasoft.wanderingtable.database.model.event

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import ru.gohasoft.wanderingtable.database.model.ObjectId

/**
 * Строка таблицы лиги.
 *
 * Хранятся только очки; **уровень не хранится, а выводится из очков** стратегией начисления.
 * Иначе появилось бы второе изменяемое поле, которое нужно держать согласованным с очками
 * при каждом начислении, — источник рассинхронизации и лишних гонок. Очки инкрементируются
 * атомарным `UPDATE ... SET points = points + :delta`.
 */
@Entity
@Table(
    name = "league_standings",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_league_standings_league_user",
            columnNames = ["league_id", "user_id"]
        )
    ]
)
data class LeagueStanding(
    @Id val id: ObjectId = ObjectId.get(),
    @Column(name = "league_id", nullable = false) val leagueId: ObjectId,
    @Column(name = "user_id", nullable = false) val userId: ObjectId,
    @Column(nullable = false) var points: Int = 0
)
