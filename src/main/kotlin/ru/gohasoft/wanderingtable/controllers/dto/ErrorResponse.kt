package ru.gohasoft.wanderingtable.controllers.dto

/**
 * Единый формат тела любой ошибки API.
 *
 * Раньше формат зависел от того, какой механизм обработал исключение (валидация отдавала
 * `{"errors": [...]}`, `ResponseStatusException` — Spring ProblemDetail, необработанные
 * исключения — Whitelabel JSON), и клиент не мог разбирать ошибки единообразно.
 */
data class ErrorResponse(
    val status: Int,
    val errors: List<String>
)
