package ru.gohasoft.wanderingtable

import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import ru.gohasoft.wanderingtable.controllers.dto.ErrorResponse

/**
 * Единая трансляция исключений в HTTP-ответы.
 *
 * До этого обрабатывалась только ошибка валидации, из-за чего неверный логин
 * (`BadCredentialsException`) отдавал 500 вместо 401, а отсутствующая запись — 500 вместо 404.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationError(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = e.bindingResult.allErrors.map { it.defaultMessage ?: "Invalid value" }
        return respond(HttpStatus.BAD_REQUEST, errors)
    }

    /** Нечитаемый JSON или неизвестное значение enum (например, несуществующая роль). */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.BAD_REQUEST, listOf("Malformed request body."))

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(e: BadCredentialsException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.UNAUTHORIZED, listOf(e.message ?: "Invalid credentials."))

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(e: AccessDeniedException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.FORBIDDEN, listOf("Access denied."))

    /** Нарушение уникального индекса: дубликат email, повторное вступление в событие и т.п. */
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(e: DataIntegrityViolationException): ResponseEntity<ErrorResponse> {
        log.debug("Constraint violation", e)
        return respond(HttpStatus.CONFLICT, listOf("Request conflicts with existing data."))
    }

    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun handleOptimisticLockFailure(e: OptimisticLockingFailureException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.CONFLICT, listOf("Concurrent modification, please retry."))

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(e: ResponseStatusException): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.valueOf(e.statusCode.value())
        return respond(status, listOf(e.reason ?: status.reasonPhrase))
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception): ResponseEntity<ErrorResponse> {
        // Наружу не отдаём ни сообщение, ни стектрейс; в лог пишем полностью.
        log.error("Unhandled exception", e)
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, listOf("Internal server error."))
    }

    private fun respond(status: HttpStatus, errors: List<String>) =
        ResponseEntity.status(status).body(ErrorResponse(status = status.value(), errors = errors))
}
