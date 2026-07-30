package ru.gohasoft.wanderingtable.database.model.event

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

/**
 * Партия: конкретная игра, которая запланирована, идёт или уже сыграна.
 *
 * Общий предок [RegularGame] и [TournamentGame] существует именно для статистики:
 * «сыграно партий», «побед» и «топ-5 любимых игр» считаются одним запросом по `game_events`,
 * независимо от того, была партия клубной или турнирной.
 */
@Entity
@Table(
    name = "game_events",
    indexes = [Index(name = "idx_game_events_starts_at", columnList = "starts_at")]
)
abstract class GameEvent : Event() {

    @Column(name = "starts_at", nullable = false)
    var startsAt: Instant = Instant.EPOCH

    @Column(name = "duration_minutes")
    var durationMinutes: Int? = null
}
