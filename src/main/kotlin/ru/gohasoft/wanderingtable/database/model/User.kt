package ru.gohasoft.wanderingtable.database.model

import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Id

@Entity
@Table(name = "users")
data class User(
    @Id val id: ObjectId = ObjectId.get(),
    val email: String,
    val hashedPassword: String,
    val role: Role = Role.Gamer
)
