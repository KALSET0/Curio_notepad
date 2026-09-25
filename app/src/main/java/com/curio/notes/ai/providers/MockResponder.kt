package com.curio.notes.ai.providers

import com.curio.notes.ai.AIResponse
import com.curio.notes.domain.model.NoteType

internal fun guessNoteType(input: String): NoteType {
    val text = input.trim().lowercase()
    return when {
        text.endsWith("?") -> NoteType.QUESTION
        text.startsWith("is it true") || text.startsWith("true or false") ||
            text.contains("studies show") || text.startsWith("i heard") ||
            text.contains("they say") -> NoteType.CLAIM
        text.contains("don't understand") || text.contains("dont understand") ||
            text.contains("confusing") || text.contains("confused") ||
            text.contains("makes no sense") -> NoteType.CONFUSION
        text.startsWith("what if") || text.startsWith("idea") ||
            text.contains("i have an idea") || text.contains("should build") ||
            text.contains("should make") -> NoteType.IDEA
        text.startsWith("i think") || text.startsWith("i feel") ||
            text.startsWith("i believe") || text.startsWith("i wonder") -> NoteType.REFLECTION
        text.startsWith("learn") || text.contains("learn about") ||
            text.startsWith("understand") || text.contains("want to understand") ||
            text.contains("research") || text.startsWith("topic") -> NoteType.TOPIC
        text.startsWith("what is") || text.startsWith("what are") ||
            text.startsWith("define") || text.startsWith("explain") ||
            text.startsWith("meaning of") -> NoteType.CONCEPT
        else -> NoteType.OTHER
    }
}

internal fun mockResponseFor(type: NoteType, input: String): AIResponse {
    val topic = topicOf(input)
    return when (type) {
        NoteType.QUESTION -> AIResponse(
            type = type,
            title = topic,
            summary = "Mock answer: a real response would answer \"$topic\" directly here in one or two sentences.",
            explanation = "Mock explanation: the reasoning behind the answer, in simple language first with room for depth. Renders the Explanation section without a network call.",
            examples = listOf("Mock example: a concrete case illustrating \"$topic\"."),
            keyPoints = listOf(
                "Mock takeaway: the core answer about \"$topic\".",
                "Mock nuance: a detail worth remembering."
            ),
            relatedTopics = listOf("Background of $topic", "$topic in practice"),
            followUpQuestions = listOf(
                "Mock follow-up: what part of \"$topic\" is still unclear?",
                "Mock follow-up: where would you apply this first?"
            )
        )
        NoteType.CONCEPT -> AIResponse(
            type = type,
            title = topic,
            summary = "Mock definition: \"$topic\" would be defined here in one sentence.",
            explanation = "Mock explanation: \"$topic\" broken down in simple language, plus why the concept matters.",
            examples = listOf("Mock example: \"$topic\" in everyday life."),
            keyPoints = listOf(
                "Mock key point: the essential meaning of \"$topic\".",
                "Mock misunderstanding: what people often get wrong about \"$topic\"."
            ),
            relatedTopics = listOf("Foundations behind $topic", "$topic vs nearby ideas"),
            followUpQuestions = listOf(
                "Mock follow-up: how would you explain \"$topic\" to a friend?",
                "Mock follow-up: what does \"$topic\" connect to?"
            )
        )
        NoteType.IDEA -> AIResponse(
            type = type,
            title = topic,
            summary = "Mock interpretation: what this idea seems to be trying to accomplish.",
            explanation = "Mock analysis: one possible implementation of \"$topic\", with potential advantages and problems weighed neutrally — never declared simply good or bad.",
            examples = listOf("Mock implementation sketch for \"$topic\"."),
            keyPoints = listOf(
                "Mock advantage: a reason this could work.",
                "Mock problem: a risk worth taking seriously.",
                "Mock improvement: a tweak that could strengthen it."
            ),
            relatedTopics = listOf("Similar attempts to $topic", "Tools for prototyping $topic"),
            followUpQuestions = listOf(
                "Mock next step: what is the smallest test of \"$topic\"?",
                "Mock follow-up: who would this help first?"
            )
        )
        NoteType.CONFUSION -> AIResponse(
            type = type,
            title = topic,
            summary = "Mock restatement: what seems confusing about \"$topic\".",
            explanation = "Mock walkthrough: the core concept behind \"$topic\", explained step by step from the ground up.",
            examples = listOf("Mock example: walking through \"$topic\" slowly."),
            keyPoints = listOf(
                "Mock core concept: the one thing \"$topic\" hinges on.",
                "Mock common misunderstanding about \"$topic\".",
                "Mock short summary: \"$topic\" in a single breath."
            ),
            relatedTopics = listOf("Prerequisites for $topic", "$topic reframed"),
            followUpQuestions = listOf(
                "Mock follow-up: which step of \"$topic\" breaks down for you?",
                "Mock follow-up: what would make \"$topic\" click?"
            )
        )
        NoteType.TOPIC -> AIResponse(
            type = type,
            title = topic,
            summary = "Mock overview: what \"$topic\" is about.",
            explanation = "Mock map: the fundamental concepts of \"$topic\", how they connect, and a suggested beginner learning path.",
            examples = listOf("Mock example: a gentle entry point into \"$topic\"."),
            keyPoints = listOf(
                "Mock starting point for \"$topic\".",
                "Mock next step once the basics click."
            ),
            relatedTopics = listOf("$topic fundamentals", "Going deeper into $topic"),
            followUpQuestions = listOf(
                "Mock follow-up: how much time can you give \"$topic\" this week?",
                "Mock follow-up: what draws you to \"$topic\"?"
            )
        )
        NoteType.CLAIM -> AIResponse(
            type = type,
            title = topic,
            summary = "Mock assessment: what is known about \"$topic\" — and what is not.",
            explanation = "Mock context: background needed to judge \"$topic\". Uncertainty is stated plainly and never presented as established fact.",
            examples = listOf("Mock example: how \"$topic\" is usually framed."),
            keyPoints = listOf(
                "Mock known point about \"$topic\".",
                "Mock uncertainty: what would settle \"$topic\"."
            ),
            relatedTopics = listOf("Evidence around $topic", "Common myths near $topic"),
            followUpQuestions = listOf(
                "Mock follow-up: what source would you trust on \"$topic\"?",
                "Mock follow-up: what would change your mind about \"$topic\"?"
            )
        )
        NoteType.REFLECTION -> AIResponse(
            type = type,
            title = topic,
            summary = "Mock interpretation: one reading of this reflection.",
            explanation = "Mock perspectives: two or three ways to look at \"$topic\". Subjective readings are not presented as objective facts.",
            examples = listOf("Mock lens: \"$topic\" seen from another angle."),
            keyPoints = listOf(
                "Mock perspective: a sympathetic reading.",
                "Mock perspective: a challenging reading."
            ),
            relatedTopics = listOf("Ideas echoing $topic", "Questions $topic raises"),
            followUpQuestions = listOf(
                "Mock question worth sitting with about \"$topic\".",
                "Mock follow-up: what experience shaped this thought?"
            )
        )
        NoteType.OTHER -> AIResponse(
            type = type,
            title = topic,
            summary = "Mock note: preserved as written.",
            explanation = "Mock response: the original intent is kept intact. A real provider would classify this and structure it once it recognizes the kind of thought it is.",
            examples = listOf("Mock example: keeping \"$topic\" exactly as captured."),
            keyPoints = listOf("Mock key point: nothing was discarded or reinterpreted."),
            relatedTopics = listOf("Things near $topic"),
            followUpQuestions = listOf(
                "Mock follow-up: what kind of thought is \"$topic\"?",
                "Mock follow-up: what should happen with this next?"
            )
        )
    }
}

internal fun topicOf(input: String, maxLength: Int = 60): String {
    val line = input.lineSequence().firstOrNull()?.trim().orEmpty()
    val cleaned = line.trimEnd('?', '.', '!', ' ').trim()
    if (cleaned.isEmpty()) return "this thought"
    return if (cleaned.length <= maxLength) cleaned else cleaned.take(maxLength).trimEnd() + "…"
}
