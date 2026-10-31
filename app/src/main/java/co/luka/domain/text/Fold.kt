package co.luka.domain.text

import java.text.Normalizer

fun stripAccents(value: String): String {
    val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
    return decomposed.replace(Regex("\\p{M}+"), "")
}

fun indexOfPhrase(text: String, phrase: String): Int {
    if (phrase.isEmpty()) return -1
    var from = 0
    while (from < text.length) {
        val index = text.indexOf(phrase, from)
        if (index < 0) return -1
        val before = index == 0 || !text[index - 1].isLetter()
        val afterIndex = index + phrase.length
        val after = afterIndex >= text.length || !text[afterIndex].isLetter()
        if (before && after) return index
        from = index + 1
    }
    return -1
}
