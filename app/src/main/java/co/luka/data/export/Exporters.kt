package co.luka.data.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import co.luka.data.db.LukaDatabase
import co.luka.data.db.Seed
import co.luka.data.db.eraseAll
import co.luka.data.settings.SettingsStore
import co.luka.domain.money.CopMoney
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class PdfReport(
    val title: String,
    val income: Long,
    val expense: Long,
    val balance: Long,
    val bars: List<Pair<String, Long>>,
)

@Singleton
class Exporters @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: LukaDatabase,
    private val settings: SettingsStore,
) {
    suspend fun csv(uri: Uri, start: String, endExclusive: String) {
        val rows = database.movementDao().exportRows(start, endExclusive)
        val body = buildString {
            appendLine("fecha,tipo,monto,categoria,comercio,entidad,nota,estado")
            rows.forEach { row ->
                appendLine(
                    listOf(
                        row.occurredDay,
                        when {
                            row.direction == "transfer" -> "traslado"
                            row.origin == "adjustment" -> "ajuste"
                            row.direction == "income" -> "ingreso"
                            else -> "gasto"
                        },
                        row.amount.toString(),
                        row.categoryName.orEmpty(),
                        row.counterparty.orEmpty(),
                        row.sourceName.orEmpty(),
                        row.note.orEmpty(),
                        row.status,
                    ).joinToString(",") { cell -> "\"" + cell.replace("\"", "\"\"") + "\"" },
                )
            }
        }
        context.contentResolver.openOutputStream(uri)?.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            ?: error("No se pudo guardar el CSV.")
    }

    fun pdf(uri: Uri, report: PdfReport) {
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        draw(page.canvas, report)
        document.finishPage(page)
        context.contentResolver.openOutputStream(uri)?.use { document.writeTo(it) }
            ?: error("No se pudo guardar el PDF.")
        document.close()
    }

    suspend fun wipe() {
        database.eraseAll()
        database.categoryDao().insertAll(Seed.categories())
        database.sourceDao().insertAll(Seed.sources())
        settings.reset()
    }

    private fun draw(canvas: Canvas, report: PdfReport) {
        val title = Paint().apply { color = Color.parseColor("#5B5BF0"); textSize = 22f; isFakeBoldText = true }
        val body = Paint().apply { color = Color.parseColor("#12141A"); textSize = 13f }
        val muted = Paint().apply { color = Color.parseColor("#6B7280"); textSize = 12f }
        val bar = Paint().apply { color = Color.parseColor("#F0526B") }
        canvas.drawText("Luka", 40f, 48f, title)
        canvas.drawText(report.title, 40f, 72f, muted)
        canvas.drawText("Ingreso ${CopMoney.format(report.income)}", 40f, 110f, body)
        canvas.drawText("Gasto ${CopMoney.format(report.expense)}", 40f, 130f, body)
        canvas.drawText("Balance ${CopMoney.format(report.balance)}", 40f, 150f, body)
        val max = report.bars.maxOfOrNull { it.second }?.takeIf { it > 0L } ?: 1L
        report.bars.take(12).forEachIndexed { index, (name, amount) ->
            val top = 190f + index * 36f
            canvas.drawText(name, 40f, top + 12f, body)
            val width = (amount.toFloat() / max.toFloat()) * 280f
            canvas.drawRect(220f, top, 220f + width, top + 16f, bar)
            canvas.drawText(CopMoney.format(amount), 510f, top + 12f, muted)
        }
        if (report.bars.isEmpty()) {
            canvas.drawText("No hay gastos en este periodo.", 40f, 200f, body)
        }
    }
}
