package ru.gohasoft.wanderingtable.database.model.event

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.Transient
import ru.gohasoft.wanderingtable.service.strategy.bracket.BracketStrategyType

/** Однодневный или двухдневный турнир: сетка партий, зачёт по победам и очкам. */
@Entity
@Table(name = "single_tournaments")
@DiscriminatorValue(SingleTournament.TYPE)
class SingleTournament : TournamentEvent() {

    @Enumerated(EnumType.STRING)
    @Column(name = "bracket_strategy", nullable = false)
    var bracketStrategy: BracketStrategyType = BracketStrategyType.ROUND_ROBIN

    @get:Transient
    override val eventType: String get() = TYPE

    companion object {
        const val TYPE = "SINGLE_TOURNAMENT"
    }
}
