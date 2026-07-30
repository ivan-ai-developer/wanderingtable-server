package ru.gohasoft.wanderingtable.database.model.event

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

/**
 * Соревнование-контейнер: само по себе не является партией, но порождает партии
 * ([TournamentGame]) по правилам своей стратегии.
 *
 * Партии ссылаются на турнирное событие, а не наоборот — благодаря этому все три подтипа
 * хранят свои партии одинаково и различаются только правилами их порождения и зачёта.
 */
@Entity
@Table(name = "tournament_events")
abstract class TournamentEvent : Event() {

    @Column(name = "starts_at", nullable = false)
    var startsAt: Instant = Instant.EPOCH

    @Column(name = "ends_at")
    var endsAt: Instant? = null

    @Column(name = "entry_fee", nullable = false, precision = 12, scale = 2)
    var entryFee: BigDecimal = BigDecimal.ZERO

    @Enumerated(EnumType.STRING)
    @Column(name = "expected_skill_level", nullable = false)
    var expectedSkillLevel: SkillLevel = SkillLevel.BEGINNER
}
