package ru.gohasoft.wanderingtable.database.model.result

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.Transient
import ru.gohasoft.wanderingtable.database.model.game.ResultType

/**
 * Результат игры, где важен только исход (например, «Манчкин»).
 *
 * Исход приходит от того, кто подводит итоги, и хранится здесь как источник правды
 * ([declaredOutcome]); базовый `outcome` — его производная копия для запросов статистики.
 * Разделение оставляет возможность позже изменить правило вывода (скажем, считать ничью
 * победой в конкретной лиге), не теряя исходные данные.
 */
@Entity
@Table(name = "win_loss_results")
@DiscriminatorValue("WIN_LOSS")
class WinLossResult : GameResult() {

    @Enumerated(EnumType.STRING)
    @Column(name = "declared_outcome", nullable = false)
    var declaredOutcome: Outcome = Outcome.UNDEFINED

    @get:Transient
    override val resultType: ResultType get() = ResultType.WIN_LOSS

    override fun resolveOutcome(peers: List<GameResult>): Outcome = declaredOutcome
}
