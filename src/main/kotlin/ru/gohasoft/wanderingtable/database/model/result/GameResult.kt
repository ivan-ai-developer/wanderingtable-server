package ru.gohasoft.wanderingtable.database.model.result

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.UniqueConstraint
import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.database.model.game.ResultType

/**
 * Результат одного игрока в одной партии. Корень JOINED-иерархии результатов.
 *
 * Поле [outcome] живёт в базовом классе намеренно: по нему считается вся статистика
 * («сыграно», «побед», «топ-5 любимых игр», таблица лиги, выбывание в чемпионате).
 * Лежи оно в подклассах — каждый такой запрос превращался бы в UNION по всем подтаблицам
 * и переписывался бы при добавлении нового типа счёта.
 *
 * При этом [outcome] — **производный кэш, а не источник правды**: он не приходит от клиента,
 * а вычисляется методом [resolveOutcome] соответствующего подкласса в единственном месте
 * (`GameEventService.finish`), поэтому рассогласоваться с исходными данными не может.
 */
@Entity
@Table(
    name = "game_results",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_game_results_event_user",
            columnNames = ["game_event_id", "user_id"]
        )
    ],
    indexes = [
        Index(name = "idx_game_results_user_id", columnList = "user_id"),
        Index(name = "idx_game_results_user_outcome", columnList = "user_id, outcome"),
        Index(name = "idx_game_results_game_event_id", columnList = "game_event_id")
    ]
)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "result_kind")
abstract class GameResult {

    @Id
    var id: ObjectId = ObjectId.get()

    @Column(name = "game_event_id", nullable = false)
    var gameEventId: ObjectId = ObjectId("")

    @Column(name = "user_id", nullable = false)
    var userId: ObjectId = ObjectId("")

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var outcome: Outcome = Outcome.UNDEFINED

    @get:Transient
    abstract val resultType: ResultType

    /**
     * Вычисляет исход из собственных данных подкласса и результатов остальных участников
     * той же партии (включая себя).
     */
    abstract fun resolveOutcome(peers: List<GameResult>): Outcome
}
