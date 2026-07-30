package com.inkwisp.app.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelConnectionSerializationTest {
    @Test
    fun connectionsSavedBeforePredictionProtocolsRemainValid() {
        val oldJson = """{
          "name":"Existing",
          "protocol":"OpenAiChat",
          "baseUrl":"https://example.test/v1",
          "modelId":"model"
        }"""

        val connection = Json.decodeFromString<ModelConnection>(oldJson)

        assertEquals(PredictionProtocol.Auto, connection.predictionProtocol)
        assertEquals(PromptFormat.Infer, connection.promptFormat)
        assertEquals("", connection.predictionBaseUrl)
        assertEquals(180, connection.predictionMaxOutputTokens)
    }
}
