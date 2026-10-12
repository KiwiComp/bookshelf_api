package com.example.bookshelfapi.core.network.model

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer

/**
 * Open Library returns a work's description either as a plain string or as
 * `{ "type": "/type/text", "value": "..." }`. Both are read as the text itself.
 */
internal object DescriptionSerializer : JsonTransformingSerializer<String>(String.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element is JsonObject) element.getValue("value") else element
}
