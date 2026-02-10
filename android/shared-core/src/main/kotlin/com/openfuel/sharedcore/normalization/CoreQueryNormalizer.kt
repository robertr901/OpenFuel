package com.openfuel.sharedcore.normalization

import java.util.Locale

private val smartApostrophesRegex = "[’‘`´]".toRegex()
private val multiplicationSymbolsRegex = "[×✕✖]".toRegex()
private val separatorRegex = "[-‐‑‒–—―_+]+".toRegex()
private val punctuationRegex = "[()\\[\\]{},;:!?…•·|]+".toRegex()
private val nonDecimalDotRegex = "(?<!\\d)\\.|\\.(?!\\d)".toRegex()
private val whitespaceRegex = "\\s+".toRegex()
private val numericPercentRegex = "(\\d)\\s*%".toRegex()
private val unitSpacingRegex = "(\\d)(?=(kg|g|ml|l|oz)\\b)".toRegex()

private val unitWordReplacements = listOf(
    "\\bgrams?\\b".toRegex(RegexOption.IGNORE_CASE) to "g",
    "\\bkilograms?\\b".toRegex(RegexOption.IGNORE_CASE) to "kg",
    "\\bmillilit(?:er|re)s?\\b".toRegex(RegexOption.IGNORE_CASE) to "ml",
    "\\blit(?:er|re)s?\\b".toRegex(RegexOption.IGNORE_CASE) to "l",
    "\\bounces?\\b".toRegex(RegexOption.IGNORE_CASE) to "oz",
    "\\bpercent\\b".toRegex(RegexOption.IGNORE_CASE) to "%",
    "\\byoghurt\\b".toRegex(RegexOption.IGNORE_CASE) to "yogurt",
)

fun normalizeSearchQuery(input: String): String {
    if (input.isBlank()) return ""

    var normalized = input
        .replace(smartApostrophesRegex, "'")
        .replace(multiplicationSymbolsRegex, "x")
        .lowercase(Locale.ROOT)

    unitWordReplacements.forEach { (pattern, replacement) ->
        normalized = normalized.replace(pattern, replacement)
    }

    normalized = normalized
        .replace(separatorRegex, " ")
        .replace(punctuationRegex, " ")
        .replace(nonDecimalDotRegex, " ")
        .replace(numericPercentRegex, "$1%")
        .replace(unitSpacingRegex, "$1 ")
        .replace(whitespaceRegex, " ")
        .trim()

    return normalized
}

fun buildNormalizedSqlLikePattern(normalizedQuery: String): String {
    if (normalizedQuery.isBlank()) return ""
    return normalizedQuery
        .split(' ')
        .filter { token -> token.isNotBlank() }
        .joinToString("%") { token -> escapeLikeToken(token) }
}

private fun escapeLikeToken(value: String): String {
    val escaped = StringBuilder(value.length + 8)
    value.forEach { char ->
        when (char) {
            '%', '_', '\\' -> {
                escaped.append('\\')
                escaped.append(char)
            }

            else -> escaped.append(char)
        }
    }
    return escaped.toString()
}
