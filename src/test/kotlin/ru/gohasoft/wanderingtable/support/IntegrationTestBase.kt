package ru.gohasoft.wanderingtable.support

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import ru.gohasoft.wanderingtable.database.model.Role
import ru.gohasoft.wanderingtable.database.repository.UserRepository
import tools.jackson.databind.ObjectMapper

/**
 * База для интеграционных тестов: настоящий PostgreSQL 15 в Docker.
 *
 * Контейнер — синглтон, запускается один раз на весь прогон и переиспользуется всеми
 * наследниками (Spring кэширует контекст, т.к. набор свойств у всех одинаков).
 * H2 здесь сознательно не используется: тесты на race condition проверяют поведение
 * `UPDATE ... WHERE` и уникальных индексов, которое в H2 отличается от PostgreSQL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTestBase {

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    protected lateinit var userRepository: UserRepository

    /**
     * Полная очистка схемы перед каждым тестом.
     *
     * Списком таблиц управляет сама БД, поэтому новые сущности не требуют правки хелпера.
     * `@Transactional` на тестах не используется: тесты гонок должны видеть реальные коммиты.
     */
    @BeforeEach
    fun cleanDatabase() {
        val tables = jdbcTemplate.queryForList(
            "select tablename from pg_tables where schemaname = 'public'",
            String::class.java
        )
        if (tables.isNotEmpty()) {
            val list = tables.joinToString(", ") { "\"$it\"" }
            jdbcTemplate.execute("truncate table $list cascade")
        }
    }

    // --- Хелперы аутентификации ---------------------------------------------------------

    protected fun register(
        email: String,
        password: String = DEFAULT_PASSWORD,
        name: String = "Test User"
    ): String {
        val result = mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("name" to name, "email" to email, "password" to password)))
        ).andReturn()
        check(result.response.status == 200) {
            "register failed: ${result.response.status} ${result.response.contentAsString}"
        }
        return result.readField("id")
    }

    protected fun login(email: String, password: String = DEFAULT_PASSWORD): TokenPair {
        val result = mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("email" to email, "password" to password)))
        ).andReturn()
        check(result.response.status == 200) {
            "login failed: ${result.response.status} ${result.response.contentAsString}"
        }
        return TokenPair(result.readField("accessToken"), result.readField("refreshToken"))
    }

    /** Регистрирует пользователя, выдаёт ему роли напрямую и возвращает свежие токены. */
    protected fun registerWithRoles(email: String, vararg roles: Role): AuthenticatedUser {
        val userId = register(email)
        if (roles.isNotEmpty()) {
            grantRoles(userId, *roles)
        }
        return AuthenticatedUser(userId, login(email))
    }

    /**
     * Выдаёт роли в обход API: у первого пользователя нет заведующего клуба,
     * который мог бы это сделать через `PATCH /users/{id}/roles`.
     */
    protected fun grantRoles(userId: String, vararg roles: Role) {
        val user = userRepository.findById(userId).orElseThrow()
        user.roles = user.roles + roles.toSet()
        userRepository.save(user)
    }

    // --- Хелперы запросов ---------------------------------------------------------------

    protected fun json(body: Any): String = objectMapper.writeValueAsString(body)

    protected fun postJson(path: String, body: Any, token: String? = null): MvcResult =
        mockMvc.perform(
            post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body))
                .apply { token?.let { header("Authorization", "Bearer $it") } }
        ).andReturn()

    protected fun patchJson(path: String, body: Any, token: String? = null): MvcResult =
        mockMvc.perform(
            patch(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body))
                .apply { token?.let { header("Authorization", "Bearer $it") } }
        ).andReturn()

    protected fun getJson(path: String, token: String? = null): MvcResult =
        mockMvc.perform(
            get(path).apply { token?.let { header("Authorization", "Bearer $it") } }
        ).andReturn()

    protected fun deleteJson(path: String, token: String? = null): MvcResult =
        mockMvc.perform(
            delete(path).apply { token?.let { header("Authorization", "Bearer $it") } }
        ).andReturn()

    protected fun MvcResult.readField(field: String): String =
        objectMapper.readTree(response.contentAsString).get(field).asString()

    protected fun MvcResult.jsonTree() = objectMapper.readTree(response.contentAsString)

    data class TokenPair(val accessToken: String, val refreshToken: String)

    data class AuthenticatedUser(val id: String, val tokens: TokenPair) {
        val accessToken: String get() = tokens.accessToken
        val refreshToken: String get() = tokens.refreshToken
    }

    companion object {
        const val DEFAULT_PASSWORD = "Password1"

        private val postgres: PostgreSQLContainer<Nothing> =
            PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:15")).apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
