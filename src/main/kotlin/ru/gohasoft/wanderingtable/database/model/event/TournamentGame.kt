package ru.gohasoft.wanderingtable.database.model.event

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Transient
import ru.gohasoft.wanderingtable.database.model.ObjectId

/**
 * Партия в рамках турнирного события.
 *
 * Наследуется от [GameEvent], поэтому попадает в статистику игрока наравне с клубными
 * партиями — иначе результаты турниров пришлось бы считать отдельным запросом.
 */
@Entity
@Table(
    name = "tournament_games",
    indexes = [
        Index(name = "idx_tournament_games_event", columnList = "tournament_event_id"),
        Index(name = "idx_tournament_games_round", columnList = "tournament_event_id, round")
    ]
)
@DiscriminatorValue(TournamentGame.TYPE)
class TournamentGame : GameEvent() {

    @Column(name = "tournament_event_id", nullable = false)
    var tournamentEventId: ObjectId = ObjectId("")

    @Column(nullable = false)
    var round: Int = 1

    @get:Transient
    override val eventType: String get() = TYPE

    companion object {
        const val TYPE = "TOURNAMENT_GAME"
    }
}
