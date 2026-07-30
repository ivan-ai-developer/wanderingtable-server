package ru.gohasoft.wanderingtable

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import ru.gohasoft.wanderingtable.support.IntegrationTestBase

class UserDeviceIntegrationTest : IntegrationTestBase() {

    private fun registerDevice(token: String, accessToken: String, platform: String = "ANDROID") =
        mockMvc.perform(
            put("/users/me/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("fcmToken" to token, "platform" to platform)))
                .header("Authorization", "Bearer $accessToken")
        ).andReturn()

    @Test
    fun `registers a device for push notifications`() {
        val user = registerWithRoles("device@example.com")

        val result = registerDevice("fcm-token-1", user.accessToken)

        assertThat(result.response.status).isEqualTo(200)
        assertThat(result.readField("platform")).isEqualTo("ANDROID")
        // Токен наружу не отдаётся — это учётные данные устройства.
        assertThat(result.response.contentAsString).doesNotContain("fcm-token-1")
    }

    @Test
    fun `keeps several devices per user`() {
        val user = registerWithRoles("multidevice@example.com")

        registerDevice("phone-token", user.accessToken)
        registerDevice("tablet-token", user.accessToken, platform = "IOS")

        val devices = getJson("/users/me/devices", user.accessToken)
        assertThat(devices.jsonTree().size()).isEqualTo(2)
    }

    @Test
    fun `re-registering the same token is idempotent`() {
        val user = registerWithRoles("idempotent@example.com")

        registerDevice("same-token", user.accessToken)
        registerDevice("same-token", user.accessToken)

        assertThat(getJson("/users/me/devices", user.accessToken).jsonTree().size()).isEqualTo(1)
    }

    /** Устройство могло перейти другому человеку — старая привязка обязана сняться. */
    @Test
    fun `token moves to the new owner`() {
        val first = registerWithRoles("firstowner@example.com")
        val second = registerWithRoles("secondowner@example.com")
        registerDevice("shared-token", first.accessToken)

        registerDevice("shared-token", second.accessToken)

        assertThat(getJson("/users/me/devices", first.accessToken).jsonTree().size()).isEqualTo(0)
        assertThat(getJson("/users/me/devices", second.accessToken).jsonTree().size()).isEqualTo(1)
    }

    @Test
    fun `unregisters a device`() {
        val user = registerWithRoles("unregister@example.com")
        registerDevice("bye-token", user.accessToken)

        val result = mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/users/me/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("fcmToken" to "bye-token")))
                .header("Authorization", "Bearer ${user.accessToken}")
        ).andReturn()

        assertThat(result.response.status).isEqualTo(204)
        assertThat(getJson("/users/me/devices", user.accessToken).jsonTree().size()).isEqualTo(0)
    }

    @Test
    fun `rejects a blank token`() {
        val user = registerWithRoles("blanktoken@example.com")

        assertThat(registerDevice("   ", user.accessToken).response.status).isEqualTo(400)
    }

    @Test
    fun `requires a token`() {
        val result = mockMvc.perform(
            put("/users/me/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(mapOf("fcmToken" to "x", "platform" to "ANDROID")))
        ).andReturn()

        assertThat(result.response.status).isEqualTo(401)
    }
}
