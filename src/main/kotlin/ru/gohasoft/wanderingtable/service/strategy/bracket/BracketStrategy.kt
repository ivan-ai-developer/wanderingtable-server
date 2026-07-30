package ru.gohasoft.wanderingtable.service.strategy.bracket

import ru.gohasoft.wanderingtable.database.model.ObjectId

/**
 * Правило порождения сетки турнирного события.
 *
 * Стратегия возвращает *план* партий, а не сущности: она остаётся чистой функцией,
 * которую можно проверить без БД, а сохранение остаётся заботой сервиса.
 */
interface BracketStrategy {
    val type: BracketStrategyType
    fun generate(participants: List<ObjectId>): List<BracketPairing>
}
