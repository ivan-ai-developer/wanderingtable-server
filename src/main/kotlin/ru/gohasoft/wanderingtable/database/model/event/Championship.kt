package ru.gohasoft.wanderingtable.database.model.event

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.Transient
import ru.gohasoft.wanderingtable.service.strategy.bracket.BracketStrategyType
import ru.gohasoft.wanderingtable.service.strategy.elimination.EliminationStrategyType

/**
 * Чемпионат: тот же турнир, но растянутый по времени и с выбыванием.
 * Состояние выбывания хранится в `ChampionshipStanding`.
 */
@Entity
@Table(name = "championships")
@DiscriminatorValue(Championship.TYPE)
class Championship : TournamentEvent() {

    @Enumerated(EnumType.STRING)
    @Column(name = "bracket_strategy", nullable = false)
    var bracketStrategy: BracketStrategyType = BracketStrategyType.ROUND_ROBIN

    @Enumerated(EnumType.STRING)
    @Column(name = "elimination_strategy", nullable = false)
    var eliminationStrategy: EliminationStrategyType = EliminationStrategyType.LOSER_ELIMINATION

    @Column(name = "current_round", nullable = false)
    var currentRound: Int = 0

    @get:Transient
    override val eventType: String get() = TYPE

    companion object {
        const val TYPE = "CHAMPIONSHIP"
    }
}
