package ru.gohasoft.wanderingtable.service.strategy.bracket

import ru.gohasoft.wanderingtable.database.model.ObjectId

/** Одна запланированная партия сетки: номер раунда и её участники. */
data class BracketPairing(
    val round: Int,
    val participants: List<ObjectId>
)