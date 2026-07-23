package ru.gohasoft.wanderingtable.controllers.utils

import org.springframework.security.core.context.SecurityContextHolder
import ru.gohasoft.wanderingtable.database.model.ObjectId

fun getCurrentObjectId(errorMessage: String) =
    (SecurityContextHolder.getContext().authentication?.principal as? String)
        ?.let(::ObjectId)
        ?: throw IllegalArgumentException(errorMessage)