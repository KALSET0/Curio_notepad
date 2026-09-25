package com.curio.notes.navigation

import kotlinx.serialization.Serializable

sealed interface CurioRoute {
    @Serializable
    data object Splash : CurioRoute

    @Serializable
    data object Home : CurioRoute

    @Serializable
    data object CreateNote : CurioRoute

    @Serializable
    data class NoteDetail(val noteId: Long) : CurioRoute

    @Serializable
    data class Conversation(val noteId: Long) : CurioRoute

    @Serializable
    data object Search : CurioRoute

    @Serializable
    data object Settings : CurioRoute
}
