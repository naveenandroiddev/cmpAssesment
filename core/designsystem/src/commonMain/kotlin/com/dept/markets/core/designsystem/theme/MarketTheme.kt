package com.dept.markets.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class MarketColors(
    val up: Color,
    val down: Color,
    val flat: Color,
    val upFlash: Color,
    val downFlash: Color,
    val gridLine: Color,
    val surfaceElevated: Color,
    val ratingBuy: Color,
    val ratingHold: Color,
    val ratingSell: Color,
)

val LocalMarketColors: ProvidableCompositionLocal<MarketColors> =
    staticCompositionLocalOf { DarkMarketColors }

val LocalMarketMetrics: ProvidableCompositionLocal<MarketMetrics> =
    staticCompositionLocalOf { MarketMetrics() }

@Immutable
data class MarketMetrics(
    val rowHeightDp: Int = 44,
    val gutterDp: Int = 12,
    val flashDurationMillis: Int = 420,
)

internal val DarkMarketColors = MarketColors(
    up = Color(0xFF3DDC97),
    down = Color(0xFFFF5C7A),
    flat = Color(0xFF8A94A6),
    upFlash = Color(0x553DDC97),
    downFlash = Color(0x55FF5C7A),
    gridLine = Color(0xFF1E2733),
    surfaceElevated = Color(0xFF131A22),
    ratingBuy = Color(0xFF3DDC97),
    ratingHold = Color(0xFFE8B84B),
    ratingSell = Color(0xFFFF5C7A),
)

internal val LightMarketColors = MarketColors(
    up = Color(0xFF0E8F5E),
    down = Color(0xFFC81E45),
    flat = Color(0xFF5B6472),
    upFlash = Color(0x330E8F5E),
    downFlash = Color(0x33C81E45),
    gridLine = Color(0xFFE2E6EC),
    surfaceElevated = Color(0xFFF6F8FA),
    ratingBuy = Color(0xFF0E8F5E),
    ratingHold = Color(0xFF9A7300),
    ratingSell = Color(0xFFC81E45),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF6FA8FF),
    background = Color(0xFF0B0F14),
    surface = Color(0xFF0F151C),
    onSurface = Color(0xFFE6EBF2),
    onSurfaceVariant = Color(0xFF9AA5B4),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF1B5FCC),
    background = Color(0xFFFBFCFE),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF10151C),
    onSurfaceVariant = Color(0xFF4A5462),
)

@Immutable
data class MarketTypography(
    val price: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
    ),
    val priceSmall: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
    ),
    val symbol: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    val caption: TextStyle = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
    ),
)

val LocalMarketTypography: ProvidableCompositionLocal<MarketTypography> =
    staticCompositionLocalOf { MarketTypography() }

@Composable
fun MarketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val marketColors = if (darkTheme) DarkMarketColors else LightMarketColors
    CompositionLocalProvider(
        LocalMarketColors provides marketColors,
        LocalMarketTypography provides MarketTypography(),
        LocalMarketMetrics provides MarketMetrics(),
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = Typography(),
            content = content,
        )
    }
}

object MarketTheme {
    val colors: MarketColors
        @Composable @ReadOnlyComposable get() = LocalMarketColors.current
    val typography: MarketTypography
        @Composable @ReadOnlyComposable get() = LocalMarketTypography.current
    val metrics: MarketMetrics
        @Composable @ReadOnlyComposable get() = LocalMarketMetrics.current
}

/** dp helpers so callers do not sprinkle magic numbers. */
val MarketMetrics.rowHeight get() = rowHeightDp.dp
val MarketMetrics.gutter get() = gutterDp.dp
