package co.luka.domain.stats

import co.luka.domain.merchant.normalizeMerchant
import co.luka.domain.money.CopMoney

data class DirectionTotals(val income: Long, val expense: Long) {
    val balance: Long get() = income - expense
}

data class NamedAmount(val name: String, val amount: Long, val id: Long? = null)

data class Delta(val percent: Double?, val increasedFromZero: Boolean)

object Stats {
    fun savingsRate(totals: DirectionTotals): Double? {
        if (totals.income == 0L) return null
        return (totals.income - totals.expense).toDouble() / totals.income.toDouble()
    }

    fun dailyAverage(expense: Long, elapsedDays: Int): Long {
        val days = if (elapsedDays < 1) 1 else elapsedDays
        return expense / days
    }

    fun monthProjection(dailyAverage: Long, daysInMonth: Int, isCurrentMonth: Boolean): Long? {
        if (!isCurrentMonth) return null
        return dailyAverage * daysInMonth
    }

    fun deltaPercent(current: Long, previous: Long): Delta = when {
        previous == 0L && current > 0L -> Delta(percent = null, increasedFromZero = true)
        previous == 0L -> Delta(percent = 0.0, increasedFromZero = false)
        else -> Delta(
            percent = (current - previous).toDouble() / previous.toDouble() * 100.0,
            increasedFromZero = false,
        )
    }

    fun largest(items: List<NamedAmount>): NamedAmount? =
        items.filter { it.amount > 0L }.maxByOrNull { it.amount }

    fun cumulative(nets: List<Long>): List<Long> {
        var running = 0L
        return nets.map { value ->
            running += value
            running
        }
    }

    fun monthlyCost(amount: Long, cadence: String): Long = when (cadence) {
        "weekly" -> amount * 52 / 12
        "yearly" -> amount / 12
        else -> amount
    }

    fun yearlyCost(amount: Long, cadence: String): Long = when (cadence) {
        "weekly" -> amount * 52
        "yearly" -> amount
        else -> amount * 12
    }

    fun insights(
        totals: DirectionTotals,
        currentCategories: List<NamedAmount>,
        previousCategories: List<NamedAmount>,
        subscriptionMonthlyCop: Long,
        expenseProjection: Long?,
    ): List<String> {
        val hasData = totals.income > 0L ||
            totals.expense > 0L ||
            currentCategories.any { it.amount > 0L } ||
            subscriptionMonthlyCop > 0L
        if (!hasData) return emptyList()

        val lines = mutableListOf<String>()
        val previous = previousCategories.associate { normalizeMerchant(it.name) to it.amount }
        val food = currentCategories.firstOrNull { normalizeMerchant(it.name) == "comida" }
        if (food != null && food.amount > 0L) {
            val before = previous["comida"] ?: 0L
            if (before > 0L && food.amount > before) {
                val percent = ((food.amount - before) * 100) / before
                lines += "Comida subió $percent% frente al periodo anterior."
            } else if (before == 0L) {
                lines += "Comida tuvo gastos y en el periodo anterior no."
            }
        }
        if (subscriptionMonthlyCop > 0L) {
            lines += "Las suscripciones suman ${CopMoney.format(subscriptionMonthlyCop)} al mes."
        }
        if (expenseProjection != null) {
            lines += if (totals.income > 0L) {
                val saving = totals.income - expenseProjection
                "Si el gasto sigue así, el ahorro del mes quedaría en ${CopMoney.format(saving)}."
            } else {
                "Si el gasto sigue así, cerrarías el mes en ${CopMoney.format(expenseProjection)}."
            }
        }
        return lines
    }
}
