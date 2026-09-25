package com.curio.notes.domain.model

import com.curio.notes.ai.AiLanguage
import java.util.Locale

enum class AppLanguage {
    SYSTEM,
    ENGLISH,
    SPANISH;

    /** Resolves the effective AI language: explicit choice wins,
     * SYSTEM follows the device locale (Spanish only when it starts with "es"). */
    fun toAiLanguage(): AiLanguage = when (this) {
        ENGLISH -> AiLanguage.ENGLISH
        SPANISH -> AiLanguage.SPANISH
        SYSTEM -> if (Locale.getDefault().language.startsWith("es")) {
            AiLanguage.SPANISH
        } else {
            AiLanguage.ENGLISH
        }
    }
}
