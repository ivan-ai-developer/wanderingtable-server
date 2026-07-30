package ru.gohasoft.wanderingtable.controllers.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import ru.gohasoft.wanderingtable.database.model.game.Game
import ru.gohasoft.wanderingtable.database.model.game.ResultType
import java.time.Instant

data class CreateGameRequest(
    @field:NotBlank(message = "Game name can't be blank.")
    @field:Size(max = 120, message = "Game name must be at most 120 characters long.")
    val name: String,
    @field:Size(max = 2000, message = "Description must be at most 2000 characters long.")
    val description: String = "",
    @field:Min(value = 1, message = "minPlayers must be at least 1.")
    val minPlayers: Int,
    @field:Min(value = 1, message = "maxPlayers must be at least 1.")
    @field:Max(value = 100, message = "maxPlayers must be at most 100.")
    val maxPlayers: Int,
    val resultType: ResultType
)

data class GameResponse(
    val id: String,
    val name: String,
    val description: String,
    val minPlayers: Int,
    val maxPlayers: Int,
    val resultType: ResultType,
    val creatorId: String,
    val createdAt: Instant
)

fun Game.toResponse() = GameResponse(
    id = id.value,
    name = name,
    description = description,
    minPlayers = minPlayers,
    maxPlayers = maxPlayers,
    resultType = resultType,
    creatorId = creatorId.value,
    createdAt = createdAt
)
