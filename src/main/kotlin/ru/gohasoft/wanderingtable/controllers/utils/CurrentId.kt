package ru.gohasoft.wanderingtable.controllers.utils

import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.server.ResponseStatusException
import ru.gohasoft.wanderingtable.database.model.ObjectId

/**
 * Идентификатор текущего пользователя из [SecurityContextHolder].
 *
 * Бросает 401, а не `IllegalArgumentException`: отсутствие аутентификации — ошибка запроса,
 * а не внутренний сбой, и раньше она превращалась в 500.
 */
fun getCurrentObjectId(): ObjectId =
    (SecurityContextHolder.getContext().authentication?.principal as? String)
        ?.let(::ObjectId)
        ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated.")
