package co.luka.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import co.luka.R

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val PlusJakarta = GoogleFont("Plus Jakarta Sans")

internal fun lukaFontFamily(gmsAvailable: Boolean): FontFamily = if (gmsAvailable) {
    FontFamily(
        Font(googleFont = PlusJakarta, fontProvider = fontProvider, weight = FontWeight.Normal),
        Font(googleFont = PlusJakarta, fontProvider = fontProvider, weight = FontWeight.Medium),
        Font(googleFont = PlusJakarta, fontProvider = fontProvider, weight = FontWeight.SemiBold),
        Font(googleFont = PlusJakarta, fontProvider = fontProvider, weight = FontWeight.Bold),
    )
} else {
    FontFamily.SansSerif
}

internal fun lukaTypography(family: FontFamily) = Typography(
    displayLarge = text(family, FontWeight.Bold, 44, 52, "tnum"),
    displayMedium = text(family, FontWeight.Bold, 40, 48, "tnum"),
    displaySmall = text(family, FontWeight.Bold, 36, 44, "tnum"),
    headlineLarge = text(family, FontWeight.SemiBold, 22, 28),
    headlineMedium = text(family, FontWeight.SemiBold, 22, 28),
    headlineSmall = text(family, FontWeight.SemiBold, 22, 28),
    titleLarge = text(family, FontWeight.SemiBold, 22, 28),
    titleMedium = text(family, FontWeight.Medium, 16, 24),
    titleSmall = text(family, FontWeight.Medium, 14, 20),
    bodyLarge = text(family, FontWeight.Normal, 16, 24),
    bodyMedium = text(family, FontWeight.Normal, 14, 20),
    bodySmall = text(family, FontWeight.Normal, 14, 20),
    labelLarge = text(family, FontWeight.Medium, 12, 16),
    labelMedium = text(family, FontWeight.Medium, 12, 16),
    labelSmall = text(family, FontWeight.Medium, 12, 16),
)

private fun text(
    family: FontFamily,
    weight: FontWeight,
    size: Int,
    line: Int,
    features: String? = null,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontFeatureSettings = features,
)
