package ru.gohasoft.wanderingtable.database.model.event

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Transient

/**
 * Обычная клубная партия — открытая заявка «собираю игру», к которой присоединяются игроки.
 * Создаётся любым пользователем с базовой ролью `PLAYER`.
 */
@Entity
@Table(name = "regular_games")
@DiscriminatorValue(RegularGame.TYPE)
class RegularGame : GameEvent() {

    @get:Transient
    override val eventType: String get() = TYPE

    companion object {
        const val TYPE = "REGULAR_GAME"
    }
}
