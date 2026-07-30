package ru.gohasoft.wanderingtable.database.model.event

enum class EventStatus {
    /** Объявлено, набирает участников — единственный статус, в котором можно вступить. */
    PLANNED,
    IN_PROGRESS,
    FINISHED,
    /** Отменено. История сохраняется, запись не удаляется. */
    CANCELLED
}
