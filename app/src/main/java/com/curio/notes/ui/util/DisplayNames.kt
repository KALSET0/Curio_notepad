package com.curio.notes.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.curio.notes.R
import com.curio.notes.domain.model.NoteStatus
import com.curio.notes.domain.model.NoteType

@Composable
fun NoteStatus.label(): String = stringResource(
    when (this) {
        NoteStatus.PENDING -> R.string.status_pending
        NoteStatus.PROCESSING -> R.string.status_processing
        NoteStatus.ANSWERED -> R.string.status_answered
        NoteStatus.ERROR -> R.string.status_error
    }
)

@Composable
fun NoteType.label(): String = stringResource(
    when (this) {
        NoteType.QUESTION -> R.string.type_question
        NoteType.CONCEPT -> R.string.type_concept
        NoteType.IDEA -> R.string.type_idea
        NoteType.CONFUSION -> R.string.type_confusion
        NoteType.TOPIC -> R.string.type_topic
        NoteType.CLAIM -> R.string.type_claim
        NoteType.REFLECTION -> R.string.type_reflection
        NoteType.OTHER -> R.string.type_other
    }
)
