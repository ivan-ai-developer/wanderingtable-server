package ru.gohasoft.wanderingtable.database.model.result

enum class Outcome {
    WIN,
    LOSS,
    DRAW,

    /** Результат ещё не подведён (партия не завершена). */
    UNDEFINED
}
