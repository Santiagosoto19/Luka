package co.luka.domain.model

enum class Direction {
    EXPENSE,
    INCOME,
    TRANSFER,
    ;

    val wire: String
        get() = when (this) {
            EXPENSE -> "expense"
            INCOME -> "income"
            TRANSFER -> "transfer"
        }

    companion object {
        fun fromWire(value: String): Direction = if (value == "income") INCOME else EXPENSE
    }
}

fun incomeKindFor(direction: Direction, categoryName: String?): String? {
    if (direction != Direction.INCOME) return null
    return if (categoryName.equals("Sueldo", ignoreCase = true)) "fixed" else "variable"
}
