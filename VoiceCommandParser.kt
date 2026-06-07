package com.voicecallassistant

object VoiceCommandParser {

    private val callPrefixes = listOf(
        "call ",
        "dial ",
        "phone ",
        "ring ",
        "contact ",
        "call up ",
        "give a call to ",
        "make a call to "
    )

    data class ParseResult(
        val action: Action,
        val contactName: String? = null,
        val rawText: String = ""
    )

    enum class Action {
        CALL_CONTACT,
        SEARCH_CONTACT,
        UNKNOWN
    }

    fun parse(spokenText: String): ParseResult {
        val normalized = spokenText.trim().lowercase()

        for (prefix in callPrefixes) {
            if (normalized.startsWith(prefix)) {
                val name = spokenText.trim()
                    .drop(prefix.length)
                    .trim()
                    .removePrefix("the ")
                    .removeSuffix(" please")
                    .removeSuffix(" now")
                    .trim()
                    .replaceFirstChar { it.uppercase() }

                if (name.isNotBlank()) {
                    return ParseResult(Action.CALL_CONTACT, name, spokenText)
                }
            }
        }

        if (normalized.startsWith("search ") || normalized.startsWith("find ") || normalized.startsWith("look for ")) {
            val query = normalized
                .removePrefix("search ")
                .removePrefix("find ")
                .removePrefix("look for ")
                .trim()
            return ParseResult(Action.SEARCH_CONTACT, query, spokenText)
        }

        return ParseResult(Action.UNKNOWN, rawText = spokenText)
    }
}
