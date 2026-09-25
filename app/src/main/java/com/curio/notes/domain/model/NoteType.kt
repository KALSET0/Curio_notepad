package com.curio.notes.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class NoteType {
    QUESTION,
    CONCEPT,
    IDEA,
    CONFUSION,
    TOPIC,
    CLAIM,
    REFLECTION,
    OTHER
}
