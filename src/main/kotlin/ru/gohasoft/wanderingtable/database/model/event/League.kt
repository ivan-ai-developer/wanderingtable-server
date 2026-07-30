package ru.gohasoft.wanderingtable.database.model.event

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.Transient
import ru.gohasoft.wanderingtable.service.strategy.league.scoring.LeagueScoringStrategyType
import java.time.Instant

/**
 * Лига: длительное мероприятие на набор очков. Партии играются любыми участниками лиги,
 * а начисление очков зависит от уровня игрока — таблица в `LeagueStanding`.
 */
@Entity
@Table(name = "leagues")
@DiscriminatorValue(League.TYPE)
class League : TournamentEvent() {

    @Enumerated(EnumType.STRING)
    @Column(name = "scoring_strategy", nullable = false)
    var scoringStrategy: LeagueScoringStrategyType = LeagueScoringStrategyType.LEVEL_WEIGHTED

    @Column(name = "season_start")
    var seasonStart: Instant? = null

    @Column(name = "season_end")
    var seasonEnd: Instant? = null

    @get:Transient
    override val eventType: String get() = TYPE

    companion object {
        const val TYPE = "LEAGUE"
    }
}
