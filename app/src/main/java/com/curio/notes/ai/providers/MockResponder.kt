package com.curio.notes.ai.providers

import com.curio.notes.ai.AIResponse
import com.curio.notes.ai.AiLanguage
import com.curio.notes.domain.model.NoteType

internal fun guessNoteType(input: String): NoteType {
    val text = input.trim().lowercase()
    return when {
        text.endsWith("?") -> NoteType.QUESTION
        text.startsWith("is it true") || text.startsWith("true or false") ||
            text.contains("studies show") || text.startsWith("i heard") ||
            text.contains("they say") ||
            text.startsWith("es verdad") || text.startsWith("es cierto") ||
            text.contains("los estudios") || text.contains("estudios muestran") ||
            text.contains("estudios demuestran") || text.startsWith("escuché") ||
            text.startsWith("escuche") || text.contains("dicen que") ||
            text.contains("me dijeron") -> NoteType.CLAIM
        text.contains("don't understand") || text.contains("dont understand") ||
            text.contains("confusing") || text.contains("confused") ||
            text.contains("makes no sense") ||
            text.contains("no entiendo") || text.contains("no comprendo") ||
            text.contains("confuso") || text.contains("confusa") ||
            text.contains("confundido") || text.contains("confundida") ||
            text.contains("no tiene sentido") ||
            text.contains("no me queda claro") -> NoteType.CONFUSION
        text.startsWith("what if") || text.startsWith("idea") ||
            text.contains("i have an idea") || text.contains("should build") ||
            text.contains("should make") ||
            text.startsWith("qué pasaría si") || text.startsWith("que pasaria si") ||
            text.startsWith("¿qué pasaría si") || text.startsWith("idea") ||
            text.contains("tengo una idea") || text.contains("se me ocurrió") ||
            text.contains("deberíamos") || text.contains("habría que") -> NoteType.IDEA
        text.startsWith("i think") || text.startsWith("i feel") ||
            text.startsWith("i believe") || text.startsWith("i wonder") ||
            text.startsWith("creo") || text.startsWith("siento") ||
            text.startsWith("pienso") || text.startsWith("me parece") ||
            text.startsWith("opino") -> NoteType.REFLECTION
        text.startsWith("learn") || text.contains("learn about") ||
            text.startsWith("understand") || text.contains("want to understand") ||
            text.contains("research") || text.startsWith("topic") ||
            text.startsWith("aprender") || text.contains("aprender sobre") ||
            text.contains("aprender de") || text.contains("quiero entender") ||
            text.contains("quiero comprender") || text.contains("investigar") ||
            text.startsWith("tema") -> NoteType.TOPIC
        text.startsWith("what is") || text.startsWith("what are") ||
            text.startsWith("define") || text.startsWith("explain") ||
            text.startsWith("meaning of") ||
            text.startsWith("qué es") || text.startsWith("que es") ||
            text.startsWith("¿qué es") || text.startsWith("qué son") ||
            text.startsWith("definición de") ||
            text.startsWith("definicion de") || text.startsWith("significado de") ||
            text.startsWith("explica") -> NoteType.CONCEPT
        else -> NoteType.OTHER
    }
}

internal fun mockResponseFor(
    type: NoteType,
    input: String,
    language: AiLanguage = AiLanguage.ENGLISH
): AIResponse = if (language == AiLanguage.SPANISH) {
    mockResponseEs(type, input)
} else {
    mockResponseEn(type, input)
}

internal fun topicOf(input: String, maxLength: Int = 60, fallback: String = "this thought"): String {
    val line = input.lineSequence().firstOrNull()?.trim().orEmpty()
    val cleaned = line.trimEnd('?', '.', '!', ' ').trim()
    if (cleaned.isEmpty()) return fallback
    return if (cleaned.length <= maxLength) cleaned else cleaned.take(maxLength).trimEnd() + "…"
}

private fun mockResponseEn(type: NoteType, input: String): AIResponse {
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

private fun mockResponseEs(type: NoteType, input: String): AIResponse {
    val topic = topicOf(input, fallback = "esta idea")
    return when (type) {
        NoteType.QUESTION -> AIResponse(
            type = type,
            title = topic,
            summary = "Respuesta simulada: una respuesta real respondería \"$topic\" directamente aquí en una o dos frases.",
            explanation = "Explicación simulada: el razonamiento detrás de la respuesta, primero en lenguaje simple y con espacio para más profundidad. Muestra la sección de explicación sin llamada de red.",
            examples = listOf("Ejemplo simulado: un caso concreto que ilustra \"$topic\"."),
            keyPoints = listOf(
                "Conclusión simulada: la respuesta central sobre \"$topic\".",
                "Matiz simulado: un detalle que vale la pena recordar."
            ),
            relatedTopics = listOf("Contexto de $topic", "$topic en la práctica"),
            followUpQuestions = listOf(
                "Seguimiento simulado: ¿qué parte de \"$topic\" sigue sin estar clara?",
                "Seguimiento simulado: ¿dónde aplicarías esto primero?"
            )
        )
        NoteType.CONCEPT -> AIResponse(
            type = type,
            title = topic,
            summary = "Definición simulada: \"$topic\" se definiría aquí en una frase.",
            explanation = "Explicación simulada: \"$topic\" desglosado en lenguaje simple, más por qué importa el concepto.",
            examples = listOf("Ejemplo simulado: \"$topic\" en la vida cotidiana."),
            keyPoints = listOf(
                "Punto clave simulado: el significado esencial de \"$topic\".",
                "Malentendido simulado: lo que la gente suele entender mal sobre \"$topic\"."
            ),
            relatedTopics = listOf("Bases detrás de $topic", "$topic frente a ideas cercanas"),
            followUpQuestions = listOf(
                "Seguimiento simulado: ¿cómo le explicarías \"$topic\" a un amigo?",
                "Seguimiento simulado: ¿con qué se conecta \"$topic\"?"
            )
        )
        NoteType.IDEA -> AIResponse(
            type = type,
            title = topic,
            summary = "Interpretación simulada: lo que esta idea parece intentar lograr.",
            explanation = "Análisis simulado: una posible implementación de \"$topic\", con posibles ventajas y problemas ponderados de forma neutral — nunca declarada simplemente buena o mala.",
            examples = listOf("Bosquejo simulado de implementación para \"$topic\"."),
            keyPoints = listOf(
                "Ventaja simulada: una razón por la que podría funcionar.",
                "Problema simulado: un riesgo que vale la pena tomar en serio.",
                "Mejora simulada: un ajuste que podría fortalecerla."
            ),
            relatedTopics = listOf("Intentos similares a $topic", "Herramientas para prototipar $topic"),
            followUpQuestions = listOf(
                "Próximo paso simulado: ¿cuál es la prueba más pequeña de \"$topic\"?",
                "Seguimiento simulado: ¿a quién ayudaría primero?"
            )
        )
        NoteType.CONFUSION -> AIResponse(
            type = type,
            title = topic,
            summary = "Replanteo simulado: lo que parece confuso de \"$topic\".",
            explanation = "Recorrido simulado: el concepto central detrás de \"$topic\", explicado paso a paso desde cero.",
            examples = listOf("Ejemplo simulado: recorriendo \"$topic\" con calma."),
            keyPoints = listOf(
                "Concepto central simulado: lo único de lo que depende \"$topic\".",
                "Malentendido común simulado sobre \"$topic\".",
                "Resumen corto simulado: \"$topic\" en una línea."
            ),
            relatedTopics = listOf("Prerrequisitos para $topic", "$topic replanteado"),
            followUpQuestions = listOf(
                "Seguimiento simulado: ¿en qué paso de \"$topic\" te pierdes?",
                "Seguimiento simulado: ¿qué haría que \"$topic\" hiciera clic?"
            )
        )
        NoteType.TOPIC -> AIResponse(
            type = type,
            title = topic,
            summary = "Panorama simulado: de qué trata \"$topic\".",
            explanation = "Mapa simulado: los conceptos fundamentales de \"$topic\", cómo se conectan, y una ruta de aprendizaje sugerida para principiantes.",
            examples = listOf("Ejemplo simulado: un punto de entrada amable a \"$topic\"."),
            keyPoints = listOf(
                "Punto de partida simulado para \"$topic\".",
                "Próximo paso simulado cuando lo básico haga clic."
            ),
            relatedTopics = listOf("Fundamentos de $topic", "Profundizando en $topic"),
            followUpQuestions = listOf(
                "Seguimiento simulado: ¿cuánto tiempo puedes dedicarle a \"$topic\" esta semana?",
                "Seguimiento simulado: ¿qué te atrae de \"$topic\"?"
            )
        )
        NoteType.CLAIM -> AIResponse(
            type = type,
            title = topic,
            summary = "Evaluación simulada: lo que se sabe sobre \"$topic\" — y lo que no.",
            explanation = "Contexto simulado: el trasfondo necesario para juzgar \"$topic\". La incertidumbre se expresa claramente y nunca se presenta como hecho establecido.",
            examples = listOf("Ejemplo simulado: cómo suele plantearse \"$topic\"."),
            keyPoints = listOf(
                "Punto conocido simulado sobre \"$topic\".",
                "Incertidumbre simulada: lo que resolvería \"$topic\"."
            ),
            relatedTopics = listOf("Evidencia alrededor de $topic", "Mitos comunes cerca de $topic"),
            followUpQuestions = listOf(
                "Seguimiento simulado: ¿en qué fuente confiarías sobre \"$topic\"?",
                "Seguimiento simulado: ¿qué te haría cambiar de opinión sobre \"$topic\"?"
            )
        )
        NoteType.REFLECTION -> AIResponse(
            type = type,
            title = topic,
            summary = "Interpretación simulada: una lectura de esta reflexión.",
            explanation = "Perspectivas simuladas: dos o tres formas de mirar \"$topic\". Las lecturas subjetivas no se presentan como hechos objetivos.",
            examples = listOf("Mirada simulada: \"$topic\" visto desde otro ángulo."),
            keyPoints = listOf(
                "Perspectiva simulada: una lectura comprensiva.",
                "Perspectiva simulada: una lectura desafiante."
            ),
            relatedTopics = listOf("Ideas que resuenan con $topic", "Preguntas que plantea $topic"),
            followUpQuestions = listOf(
                "Pregunta simulada con la que vale la pena quedarse sobre \"$topic\".",
                "Seguimiento simulado: ¿qué experiencia dio forma a esta idea?"
            )
        )
        NoteType.OTHER -> AIResponse(
            type = type,
            title = topic,
            summary = "Nota simulada: preservada como fue escrita.",
            explanation = "Respuesta simulada: la intención original se mantiene intacta. Un proveedor real clasificaría esto y lo estructuraría cuando reconociera qué tipo de idea es.",
            examples = listOf("Ejemplo simulado: manteniendo \"$topic\" tal como se capturó."),
            keyPoints = listOf("Punto clave simulado: nada se descartó ni se reinterpretó."),
            relatedTopics = listOf("Cosas cercanas a $topic"),
            followUpQuestions = listOf(
                "Seguimiento simulado: ¿qué tipo de idea es \"$topic\"?",
                "Seguimiento simulado: ¿qué debería pasar con esto ahora?"
            )
        )
    }
}
