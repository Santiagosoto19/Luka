package co.luka.domain.merchant

import co.luka.domain.text.stripAccents

fun normalizeMerchant(name: String): String =
    stripAccents(name.trim().lowercase()).replace(Regex("\\s+"), " ").trim()
