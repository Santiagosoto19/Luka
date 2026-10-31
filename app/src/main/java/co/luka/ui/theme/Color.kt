package co.luka.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

internal val Ink = Color(0xFF12141A)
internal val InkDark = Color(0xFFF4F5F7)
internal val Paper = Color(0xFFFFFFFF)
internal val Night = Color(0xFF0E1014)
internal val CardLight = Color(0xFFF7F8FA)
internal val CardDark = Color(0xFF171A21)
internal val BorderLight = Color(0xFFECEEF2)
internal val BorderDark = Color(0xFF232733)
internal val MutedLight = Color(0xFF6B7280)
internal val MutedDark = Color(0xFF9CA3AF)
internal val TertiaryLight = Color(0xFFA1A7B3)
internal val PrimaryLight = Color(0xFF5B5BF0)
internal val PrimaryDark = Color(0xFF7A7AF5)
internal val SecondaryLight = Color(0xFF8B7CF6)
internal val SecondaryDark = Color(0xFFA094F8)
internal val IncomeLight = Color(0xFF12B886)
internal val IncomeDark = Color(0xFF3DDBA8)
internal val IncomeContainerLight = Color(0xFFE6F8F1)
internal val IncomeContainerDark = Color(0xFF14382F)
internal val ExpenseLight = Color(0xFFF0526B)
internal val ExpenseDark = Color(0xFFFF7A8C)
internal val ExpenseContainerLight = Color(0xFFFDECEF)
internal val ExpenseContainerDark = Color(0xFF3D242C)
internal val WarningLight = Color(0xFFF5A524)
internal val WarningDark = Color(0xFFF8B03E)
internal val InfoLight = Color(0xFF38BDF8)
internal val InfoDark = Color(0xFF56C7F9)
internal val ChipContainerLight = Color(0xFFF4F3FE)
internal val ChipContainerDark = Color(0xFF2A2848)
internal val OnInk = Color(0xFFFFFFFF)

internal val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnInk,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = OnInk,
    inversePrimary = PrimaryDark,
    secondary = SecondaryLight,
    onSecondary = Ink,
    secondaryContainer = ChipContainerLight,
    onSecondaryContainer = PrimaryLight,
    tertiary = InfoLight,
    onTertiary = Ink,
    background = Paper,
    onBackground = Ink,
    surface = CardLight,
    onSurface = Ink,
    surfaceVariant = CardLight,
    onSurfaceVariant = MutedLight,
    surfaceTint = Color.Transparent,
    inverseSurface = Ink,
    inverseOnSurface = Paper,
    error = ExpenseLight,
    onError = OnInk,
    errorContainer = ExpenseContainerLight,
    onErrorContainer = Ink,
    outline = BorderLight,
    outlineVariant = BorderLight,
    surfaceBright = Paper,
    surfaceDim = CardLight,
    surfaceContainerLowest = Paper,
    surfaceContainerLow = Paper,
    surfaceContainer = CardLight,
    surfaceContainerHigh = Paper,
    surfaceContainerHighest = CardLight,
)

internal val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Ink,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = Ink,
    inversePrimary = PrimaryLight,
    secondary = SecondaryDark,
    onSecondary = Ink,
    secondaryContainer = ChipContainerDark,
    onSecondaryContainer = SecondaryDark,
    tertiary = InfoDark,
    onTertiary = Ink,
    background = Night,
    onBackground = InkDark,
    surface = CardDark,
    onSurface = InkDark,
    surfaceVariant = CardDark,
    onSurfaceVariant = MutedDark,
    surfaceTint = Color.Transparent,
    inverseSurface = InkDark,
    inverseOnSurface = Ink,
    error = ExpenseDark,
    onError = Ink,
    errorContainer = ExpenseContainerDark,
    onErrorContainer = InkDark,
    outline = BorderDark,
    outlineVariant = BorderDark,
    surfaceBright = CardDark,
    surfaceDim = Night,
    surfaceContainerLowest = CardDark,
    surfaceContainerLow = CardDark,
    surfaceContainer = CardDark,
    surfaceContainerHigh = CardDark,
    surfaceContainerHighest = CardDark,
)

@Immutable
data class LukaColors(
    val income: Color,
    val expense: Color,
    val incomeContainer: Color,
    val expenseContainer: Color,
    val warning: Color,
    val info: Color,
    val muted: Color,
    val tertiary: Color,
    val border: Color,
    val displayFont: FontFamily,
)

val LocalLukaColors = staticCompositionLocalOf {
    LukaColors(
        income = IncomeLight,
        expense = ExpenseLight,
        incomeContainer = IncomeContainerLight,
        expenseContainer = ExpenseContainerLight,
        warning = WarningLight,
        info = InfoLight,
        muted = MutedLight,
        tertiary = TertiaryLight,
        border = BorderLight,
        displayFont = FontFamily.SansSerif,
    )
}

internal fun lukaColors(dark: Boolean, displayFont: FontFamily) = if (dark) {
    LukaColors(
        income = IncomeDark,
        expense = ExpenseDark,
        incomeContainer = IncomeContainerDark,
        expenseContainer = ExpenseContainerDark,
        warning = WarningDark,
        info = InfoDark,
        muted = MutedDark,
        tertiary = TertiaryLight,
        border = BorderDark,
        displayFont = displayFont,
    )
} else {
    LukaColors(
        income = IncomeLight,
        expense = ExpenseLight,
        incomeContainer = IncomeContainerLight,
        expenseContainer = ExpenseContainerLight,
        warning = WarningLight,
        info = InfoLight,
        muted = MutedLight,
        tertiary = TertiaryLight,
        border = BorderLight,
        displayFont = displayFont,
    )
}

const val FallbackCategoryHex = "#6B7280"

fun colorFromHex(hex: String): Color {
    val clean = hex.removePrefix("#")
    val value = clean.toLong(16)
    return if (clean.length <= 6) Color(0xFF000000 or value) else Color(value)
}
