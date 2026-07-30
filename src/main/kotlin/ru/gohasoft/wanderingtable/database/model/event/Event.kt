package ru.gohasoft.wanderingtable.database.model.event

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.Version
import ru.gohasoft.wanderingtable.database.model.ObjectId
import ru.gohasoft.wanderingtable.database.model.nowTruncated
import java.time.Instant

/**
 * Корень иерархии событий клуба.
 *
 * ```
 * Event
 * ├── GameEvent        — партия (источник статистики игрока)
 * │   ├── RegularGame       — свободная заявка на партию
 * │   └── TournamentGame    — партия в рамках турнирного события
 * └── TournamentEvent  — соревнование-контейнер
 *     ├── SingleTournament
 *     ├── Championship
 *     └── League
 * ```
 *
 * Стратегия [InheritanceType.JOINED]: общие поля живут в `events`, у каждого подкласса своя
 * таблица с FK на `events.id`. Это позволяет объявлять NOT NULL на поля подкласса
 * (в single-table они были бы обязаны быть nullable) и добавлять новые типы событий
 * без изменения существующих таблиц.
 *
 * Свойства объявлены в теле класса, а не в конструкторе, намеренно: noarg-плагин Kotlin
 * не генерирует конструктор без аргументов для абстрактных классов, из-за чего Hibernate
 * не мог создать ни один подкласс («No default constructor for entity»). Класс без
 * параметров конструктора получает такой конструктор сам.
 */
@Entity
@Table(
    name = "events",
    indexes = [
        Index(name = "idx_events_status", columnList = "status"),
        Index(name = "idx_events_game_id", columnList = "game_id"),
        Index(name = "idx_events_creator_id", columnList = "creator_id")
    ]
)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "event_type")
abstract class Event {

    @Id
    var id: ObjectId = ObjectId.get()

    var title: String = ""

    var description: String = ""

    /** Ссылка на запись справочника `Game`. */
    @Column(name = "game_id", nullable = false)
    var gameId: ObjectId = ObjectId("")

    @Column(name = "creator_id", nullable = false)
    var creatorId: ObjectId = ObjectId("")

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: EventStatus = EventStatus.PLANNED

    @Column(name = "min_participants", nullable = false)
    var minParticipants: Int = 1

    @Column(name = "max_participants", nullable = false)
    var maxParticipants: Int = 1

    /**
     * Денормализованное число участников.
     *
     * Нужно именно как столбец, а не `count(*)` по `event_participants`: вступление
     * выполняется атомарным `UPDATE ... WHERE participants_count < max_participants`,
     * что и защищает от переполнения события при одновременных запросах.
     * Единственный источник правды по составу участников — таблица `event_participants`;
     * этот счётчик обновляется в той же транзакции.
     */
    @Column(name = "participants_count", nullable = false)
    var participantsCount: Int = 0

    var createdAt: Instant = nowTruncated()

    @Version
    var version: Long = 0

    /** Значение дискриминатора; отдаётся клиенту как поле `type`. */
    @get:Transient
    abstract val eventType: String
}
