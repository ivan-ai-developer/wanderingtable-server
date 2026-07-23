# Wandering Table — API для Android-клиента (Kotlin)

Документация по взаимодействию мобильного приложения с backend-сервером Wandering Table.
Актуальна на основе исходного кода сервера в `src/main/kotlin/ru/gohasoft/wanderingtable`.

## Базовые сведения

- **Base URL (dev):** `http://<host>:8050`
  - Порт задан в `application.properties` (`server.port=8050`), context-path отсутствует — все пути идут от корня.
  - Для эмулятора Android base URL localhost-бэкенда — `http://10.0.2.2:8050`, для физического устройства — IP машины с сервером в локальной сети.
- **Формат данных:** JSON (`Content-Type: application/json`) для тела запроса/ответа.
- **Аутентификация:** JWT (access + refresh токены), передаётся в заголовке `Authorization: Bearer <accessToken>`.
- **CSRF:** отключён (это чистый REST API), поэтому CSRF-токен не нужен.
- **Публичные (без токена) маршруты:** `GET /`, всё под `/auth/**`. Все остальные запросы требуют валидный access-токен.

---

## 1. Аутентификация

### 1.1 Регистрация — `POST /auth/register`

Тело запроса:

```json
{
  "email": "user@example.com",
  "password": "Password1"
}
```

Валидация (проверяется на сервере, ошибки — см. раздел "Ошибки валидации"):

- `email` — должен быть валидным email-адресом.
- `password` — минимум 8 символов, обязательно хотя бы одна строчная буква, одна заглавная и одна цифра.
  Регулярка сервера: `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$`

Ответ: **200 OK**, тело пустое (сервер ничего не возвращает).
Если пользователь с таким email уже существует — **409 Conflict** с телом:

```json
{ "error": "A user with that email already exists." }
```

> Примечание: точный формат тела 409-ответа зависит от `ResponseStatusException` — в теле будет строка `reason`, оформленная стандартным Spring `ProblemDetail`/error JSON (см. раздел про ошибки ниже).

После успешной регистрации нужно отдельно вызвать `/auth/login` — регистрация не возвращает токены.

### 1.2 Логин — `POST /auth/login`

Тело запроса:

```json
{
  "email": "user@example.com",
  "password": "Password1"
}
```

Ответ: **200 OK**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

Ошибка (неверный email/пароль): **401 Unauthorized**, тело пустое (стандартный `HttpStatusEntryPoint`, JSON не гарантирован).

### 1.3 Обновление токенов — `POST /auth/refresh`

Тело запроса:

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

Ответ: **200 OK**, та же структура, что и у `/auth/login`:

```json
{
  "accessToken": "новый access token",
  "refreshToken": "новый refresh token"
}
```

Ошибка: **401 Unauthorized** с телом вида:

```json
{ "error": "Invalid refresh token." }
```

или `"Refresh token not recognized (maybe used or expired?)"`.

**Важно — refresh-токен одноразовый (rotation):**
При каждом обращении к `/auth/refresh` старый refresh-токен удаляется на сервере, и выдаётся новая пара `accessToken` + `refreshToken`. Клиент обязан **сохранить новый refreshToken** и больше не использовать старый — повторный вызов со старым токеном вернёт 401.

### Время жизни токенов

| Токен | TTL |
|---|---|
| Access token | 15 минут |
| Refresh token | 30 дней |

Отсюда практическая стратегия для клиента:
1. Access-токен использовать для всех запросов.
2. При получении 401 на защищённом эндпоинте — попытаться обновить пару через `/auth/refresh`, и повторить исходный запрос один раз.
3. Если `/auth/refresh` тоже вернул 401 — считать сессию истёкшей, разлогинить пользователя и отправить на экран логина.

---

## 2. Новостные записи (NewsNote) — требуют авторизации

> Ранее эта сущность называлась «Заметки» (`Note`), в коде переименована в `NewsNote`. URL-путь при этом не менялся и остался `/notes`.

Все эндпоинты ниже требуют заголовок:

```
Authorization: Bearer <accessToken>
```

Владелец записи (`ownerId`) определяется сервером из `sub` (userId) внутри JWT — передавать `ownerId` в запросе не нужно и не имеет смысла.

### 2.1 Создание / обновление записи — `POST /notes`

Тело запроса:

```json
{
  "id": null,
  "title": "Заголовок",
  "content": "Текст новости"
}
```

- `id` — необязателен. Если передать `id` существующей записи — произойдёт upsert (запись обновится). Если `null` или запись с таким `id` не найдена — создастся новая запись с этим (или сгенерированным) id.
- `title` — обязателен, не должен быть пустым (`@NotBlank`).
- `content` — строка, может быть пустой.
- Поля `color` в запросе больше нет (было в более ранней версии API и нигде не сохранялось на сервере — убрано из модели запроса).

Ответ: **200 OK**

```json
{
  "id": "1a2b3c4d5e6f",
  "title": "Заголовок",
  "content": "Текст новости",
  "createdAt": "2026-07-23T10:15:30Z"
}
```

`id` — строка (генерируется как hex от случайного `Long`, не UUID и не Mongo ObjectId, несмотря на название класса `ObjectId`).
`createdAt` — ISO-8601 Instant (UTC). При обновлении существующей записи (`id` найден) `createdAt` **сохраняется исходным**, обновляются только `title`/`content`.

Проверка владельца при обновлении: если `id` в запросе принадлежит записи **другого** пользователя, сервер вернёт **403 Forbidden** и ничего не изменит:

```json
{ "error": "Not allowed to modify this note." }
```

Клиенту стоит обрабатывать `403` на этом эндпоинте — например, как признак рассинхронизации локального кэша (запись была удалена и id переиспользован кем-то другим, что маловероятно) или как программную ошибку (передан чужой/некорректный id).

### 2.2 Получение своих записей — `GET /notes`

Без query-параметров — список новостных записей текущего пользователя (по `ownerId` из токена).

Ответ: **200 OK**

```json
[
  {
    "id": "1a2b3c4d5e6f",
    "title": "Заголовок",
    "content": "Текст новости",
    "createdAt": "2026-07-23T10:15:30Z"
  }
]
```

### 2.3 Удаление записи — `DELETE /notes/{id}`

Ответ: **200 OK**, тело пустое.

Поведение:
- Если запись с таким `id` не найдена — **500** (`IllegalArgumentException("Note not found")`, не превращается в аккуратный JSON, см. ниже).
- Если запись найдена, но принадлежит другому пользователю — сервер **тихо ничего не удаляет и всё равно возвращает 200 OK**. Клиенту нельзя полагаться на статус-код как индикатор успеха удаления чужой записи; такого сценария в норме быть не должно, т.к. UI не должен показывать чужие записи.

---

## 3. Проверка доступности сервера

### `GET /`

Публичный эндпоинт, без авторизации. Ответ: **200 OK**, тело — обычная строка (не JSON):

```
Everything cool!
```

Удобно для health-check / определения, поднят ли сервер, перед показом экрана логина.

---

## 4. Формат ошибок

Сервер не имеет единого консистентного формата ошибок — учитывайте разные случаи:

1. **Ошибки валидации** (`400 Bad Request`) — на `/auth/register` при некорректном email/паролю:

   ```json
   { "errors": ["Invalid email format.", "Password must be at least 8 characters long and contain at least one digit, uppercase and lowercase character."] }
   ```

2. **Бизнес-ошибки через `ResponseStatusException`** (409 при регистрации дубликата, 401 при невалидном refresh-токене) — стандартный Spring error body с полем `error`/`message`/`status`/`timestamp`/`path` (зависит от конфигурации `server.error.include-message`, по умолчанию поле `message` может быть скрыто). Рекомендуется на клиенте ориентироваться в первую очередь на **HTTP-статус**, а не парсить текст сообщения.

3. **401 от Spring Security** (неверные логин/пароль, отсутствующий/невалидный access-токен на защищённых маршрутах) — тело обычно пустое.

4. **Необработанные исключения** (например, удаление несуществующей новостной записи) — **500 Internal Server Error** со стандартной Spring Boot Whitelabel error JSON-структурой:

   ```json
   {
     "timestamp": "...",
     "status": 500,
     "error": "Internal Server Error",
     "path": "/notes/xyz"
   }
   ```

**Рекомендация для Android-клиента:** обрабатывать в первую очередь коды `401`, `409`, `400`, `500`, и не завязываться жёстко на текст сообщений — сервер их не гарантирует в едином виде.

---

## 5. Пример интеграции на Kotlin (Retrofit + OkHttp)

### 5.1 Retrofit-интерфейс

```kotlin
interface WanderingTableApi {

    @POST("auth/register")
    suspend fun register(@Body body: AuthRequest)

    @POST("auth/login")
    suspend fun login(@Body body: AuthRequest): TokenPairDto

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenPairDto

    @GET("notes")
    suspend fun getNewsNotes(): List<NewsNoteDto>

    @POST("notes")
    suspend fun saveNewsNote(@Body body: NewsNoteRequestDto): NewsNoteDto

    @DELETE("notes/{id}")
    suspend fun deleteNewsNote(@Path("id") id: String)
}

data class AuthRequest(val email: String, val password: String)
data class RefreshRequest(val refreshToken: String)
data class TokenPairDto(val accessToken: String, val refreshToken: String)

data class NewsNoteRequestDto(
    val id: String? = null,
    val title: String,
    val content: String
)

data class NewsNoteDto(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: String // instant в ISO-8601, парсить через java.time.Instant.parse
)
```

### 5.2 Хранение токенов

Хранить `accessToken`/`refreshToken` **только** в `EncryptedSharedPreferences` (Jetpack Security) — они дают доступ ко всем данным пользователя, хранить их в обычных `SharedPreferences` или логировать не следует.

```kotlin
class TokenStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "auth_tokens",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var accessToken: String?
        get() = prefs.getString("access_token", null)
        set(value) = prefs.edit { putString("access_token", value) }

    var refreshToken: String?
        get() = prefs.getString("refresh_token", null)
        set(value) = prefs.edit { putString("refresh_token", value) }

    fun clear() = prefs.edit { clear() }
}
```

### 5.3 OkHttp Authenticator для автообновления токена

Refresh-токен одноразовый (rotation), поэтому обновление должно быть атомарным — используйте `synchronized`/мьютекс, чтобы при нескольких параллельных 401 не отправить старый refreshToken дважды.

```kotlin
class TokenAuthenticator(
    private val tokenStore: TokenStore,
    private val authApiProvider: () -> WanderingTableApi // без interceptor'а авторизации, отдельный клиент
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Не пытаться обновлять токен бесконечно
        if (responseCount(response) >= 2) return null

        synchronized(lock) {
            val currentAccessToken = tokenStore.accessToken
            // Если токен уже обновился в другом потоке — просто повторяем запрос с новым
            val headerToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            if (currentAccessToken != null && headerToken != currentAccessToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            val refreshToken = tokenStore.refreshToken ?: return null

            return try {
                val newTokens = runBlocking {
                    authApiProvider().refresh(RefreshRequest(refreshToken))
                }
                tokenStore.accessToken = newTokens.accessToken
                tokenStore.refreshToken = newTokens.refreshToken

                response.request.newBuilder()
                    .header("Authorization", "Bearer ${newTokens.accessToken}")
                    .build()
            } catch (e: Exception) {
                tokenStore.clear() // refresh не удался — сессия истекла, разлогиниваем
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
```

### 5.4 Interceptor для проставления access-токена

```kotlin
class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenStore.accessToken ?: return chain.proceed(original)
        val authorized = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(authorized)
    }
}
```

Собрать `OkHttpClient`:

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(AuthInterceptor(tokenStore))
    .authenticator(TokenAuthenticator(tokenStore) { plainAuthApi })
    .build()
```

---

## 6. Известные ограничения текущей реализации сервера

Эти моменты полезно учитывать при проектировании клиента, чтобы не полагаться на несуществующее поведение:

- Endpoint `POST /notes` не разделяет "создать" и "обновить" — это единый upsert по `id`. `createdAt` **сохраняется** при обновлении существующей записи (не пересчитывается).
- `POST /notes` при обновлении чужой записи по `id` возвращает **403 Forbidden** (IDOR исправлен) — обрабатывайте этот код на клиенте.
- `DELETE /notes/{id}` для чужой записи возвращает 200, хотя ничего не удаляет (в отличие от `POST`, здесь просто нет-оп вместо 403) — не показывайте пользователю чужие id.
- Тело ответа на ошибки не унифицировано (см. раздел 4) — код клиента должен опираться на HTTP-статус, а не на структуру JSON ошибки.
- Роль пользователя (`Role`: `Maintainer`, `Admin`, `Gamer`) хранится в БД, но **не передаётся клиенту** ни в токене, ни в ответах API — если понадобится ролевая логика на клиенте, потребуется доработка backend.
