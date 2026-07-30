package ru.gohasoft.wanderingtable.controllers.dto

import org.springframework.data.domain.Page

/**
 * Стабильный формат постраничного ответа.
 *
 * Spring-овый `PageImpl` наружу не отдаётся: его JSON-представление объявлено нестабильным
 * и может меняться между версиями Spring Data, что ломало бы Android-клиент.
 */
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

fun <T : Any, R> Page<T>.toResponse(map: (T) -> R) = PageResponse(
    content = content.map(map),
    page = number,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages
)
