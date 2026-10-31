package co.luka.data.db

import co.luka.capture.parser.BancolombiaParser
import co.luka.capture.parser.NequiParser
import co.luka.capture.parser.NuParser

object Seed {
    fun categories(): List<CategoryEntity> {
        val expense = listOf(
            Triple("Comida", "#E36B45", "restaurant"),
            Triple("Transporte", "#3D7A73", "bus"),
            Triple("Hogar", "#C4844A", "home"),
            Triple("Servicios", "#4C7C9B", "bolt"),
            Triple("Salud", "#C45B6A", "health"),
            Triple("Compras", "#7A6A4F", "bag"),
            Triple("Entretenimiento", "#6B5B95", "movie"),
            Triple("Suscripciones", "#1AAE9F", "repeat"),
            Triple("Otros", "#8E9C98", "category"),
        )
        val income = listOf(
            Triple("Sueldo", "#1AAE9F", "payments"),
            Triple("Ventas", "#2E8B74", "store"),
            Triple("Extra", "#F4C95D", "spark"),
            Triple("Otros ingresos", "#8E9C98", "wallet"),
        )
        return expense.mapIndexed { index, (name, color, icon) ->
            CategoryEntity(
                name = name,
                direction = "expense",
                icon = icon,
                color = color,
                sortOrder = index,
                isSystem = true,
                archived = false,
            )
        } + income.mapIndexed { index, (name, color, icon) ->
            CategoryEntity(
                name = name,
                direction = "income",
                icon = icon,
                color = color,
                sortOrder = index,
                isSystem = true,
                archived = false,
            )
        }
    }

    fun sources(): List<SourceEntity> = listOf(
        SourceEntity(
            slug = "cash",
            name = "Efectivo",
            kind = "cash",
            packageName = null,
            parserId = null,
            color = "#8E9C98",
            icon = "payments",
            monitorNotifications = false,
            smsSender = null,
            sortOrder = 0,
            archived = false,
        ),
        SourceEntity(
            slug = "nequi",
            name = "Nequi",
            kind = "app",
            packageName = NequiParser.PACKAGE_NAME,
            parserId = NequiParser.id,
            color = "#E36B45",
            icon = "wallet",
            monitorNotifications = true,
            smsSender = null,
            sortOrder = 1,
            archived = false,
        ),
        SourceEntity(
            slug = "nu",
            name = "Nu",
            kind = "app",
            packageName = NuParser.PACKAGE_NAME,
            parserId = NuParser.id,
            color = "#6B5B95",
            icon = "spark",
            monitorNotifications = true,
            smsSender = null,
            sortOrder = 2,
            archived = false,
        ),
        SourceEntity(
            slug = "bancolombia",
            name = "Bancolombia",
            kind = "app",
            packageName = BancolombiaParser.PACKAGE_NAME,
            parserId = BancolombiaParser.id,
            color = "#F4C95D",
            icon = "home",
            monitorNotifications = true,
            smsSender = null,
            sortOrder = 3,
            archived = false,
        ),
    )
}
