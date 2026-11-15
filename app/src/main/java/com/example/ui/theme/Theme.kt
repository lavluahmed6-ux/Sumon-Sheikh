package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class DokanSemanticColors(
    val salesBlue: Color,
    val salesBlueContainer: Color,
    val paymentGreen: Color,
    val paymentGreenContainer: Color,
    val dueRed: Color,
    val dueRedContainer: Color,
    val dueOrange: Color,
    val dueOrangeContainer: Color,
    val expenseAmber: Color,
    val expenseContainer: Color,
    val ledgerTeal: Color,
    val ledgerTealContainer: Color,
    val cardBorder: Color
)

private val LightSemanticColors = DokanSemanticColors(
    salesBlue = SalesBlueLight,
    salesBlueContainer = SalesBlueContainerLight,
    paymentGreen = PaymentGreenLight,
    paymentGreenContainer = PaymentGreenContainerLight,
    dueRed = DueRedLight,
    dueRedContainer = DueRedContainerLight,
    dueOrange = DueOrangeLight,
    dueOrangeContainer = DueOrangeContainerLight,
    expenseAmber = ExpenseAccentLight,
    expenseContainer = ExpenseContainerLight,
    ledgerTeal = LedgerTealLight,
    ledgerTealContainer = LedgerTealContainerLight,
    cardBorder = OutlineLight
)

private val DarkSemanticColors = DokanSemanticColors(
    salesBlue = SalesBlueDark,
    salesBlueContainer = SalesBlueContainerDark,
    paymentGreen = PaymentGreenDark,
    paymentGreenContainer = PaymentGreenContainerDark,
    dueRed = DueRedDark,
    dueRedContainer = DueRedContainerDark,
    dueOrange = DueOrangeDark,
    dueOrangeContainer = DueOrangeContainerDark,
    expenseAmber = ExpenseAccentDark,
    expenseContainer = ExpenseContainerDark,
    ledgerTeal = LedgerTealDark,
    ledgerTealContainer = LedgerTealContainerDark,
    cardBorder = OutlineDark
)

val LocalDokanColors = staticCompositionLocalOf { LightSemanticColors }

private val LightColorScheme = lightColorScheme(
    primary = SalesBlueLight,
    onPrimary = Color.White,
    primaryContainer = SalesBlueContainerLight,
    onPrimaryContainer = Color(0xFF0A2E5C),
    secondary = PaymentGreenLight,
    onSecondary = Color.White,
    secondaryContainer = PaymentGreenContainerLight,
    onSecondaryContainer = Color(0xFF0A361A),
    tertiary = DueOrangeLight,
    onTertiary = Color.White,
    tertiaryContainer = DueOrangeContainerLight,
    onTertiaryContainer = Color(0xFF452100),
    error = DueRedLight,
    onError = Color.White,
    errorContainer = DueRedContainerLight,
    onErrorContainer = Color(0xFF5C1108),
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    outlineVariant = OutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = SalesBlueDark,
    onPrimary = Color(0xFF002752),
    primaryContainer = SalesBlueContainerDark,
    onPrimaryContainer = Color(0xFFD6E7FF),
    secondary = PaymentGreenDark,
    onSecondary = Color(0xFF033015),
    secondaryContainer = PaymentGreenContainerDark,
    onSecondaryContainer = Color(0xFFBBF7D0),
    tertiary = DueOrangeDark,
    onTertiary = Color(0xFF3E2200),
    tertiaryContainer = DueOrangeContainerDark,
    onTertiaryContainer = Color(0xFFFDE68A),
    error = DueRedDark,
    onError = Color(0xFF4C0510),
    errorContainer = DueRedContainerDark,
    onErrorContainer = Color(0xFFFFDAD6),
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    outlineVariant = OutlineDark
)

private val DokanShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

object DokanTheme {
    val colors: DokanSemanticColors
        @Composable get() = LocalDokanColors.current
}

@Composable
fun DokanHisabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(LocalDokanColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = DokanShapes,
            content = content
        )
    }
}
