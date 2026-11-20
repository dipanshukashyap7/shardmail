package com.shardmail.app.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

// PART 0 — MATERIAL YOU (M3 EXPRESSIVE) COLOR TOKENS (LIGHT ONLY)
val Primary = Color(0xFF6750A4)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFEADDFF)
val OnPrimaryContainer = Color(0xFF21005D)

val Secondary = Color(0xFF625B71)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFE8DEF8)
val OnSecondaryContainer = Color(0xFF1D192B)

val Tertiary = Color(0xFF7D5260)
val TertiaryContainer = Color(0xFFFFD8E4)
val OnTertiaryContainer = Color(0xFF31111D)

val Error = Color(0xFFB3261E)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFF9DEDC)
val OnErrorContainer = Color(0xFF410E0B)

val Surface = Color(0xFFFEF7FF)
val OnSurface = Color(0xFF1D1B20)
val SurfaceVariant = Color(0xFFE7E0EC)
val OnSurfaceVariant = Color(0xFF49454F)

val SurfaceDim = Color(0xFFDED8E1)
val SurfaceBright = Color(0xFFFEF7FF)

val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerLow = Color(0xFFF7F2FA)
val SurfaceContainer = Color(0xFFF3EDF7)
val SurfaceContainerHigh = Color(0xFFECE6F0)
val SurfaceContainerHighest = Color(0xFFE6E0E9)

val Outline = Color(0xFF79747E)
val OutlineVariant = Color(0xFFCAC4D0)

val InverseSurface = Color(0xFF322F35)
val InverseOnSurface = Color(0xFFF5EFF7)
val InversePrimary = Color(0xFFD0BCFF)

val Scrim = Color(0x52000000)   // rgba(0,0,0,0.32)
val Shadow = Color(0x2421005D)  // rgba(33,0,93,0.14)

// Hairline divider token: outlineVariant at 35-50% opacity
val HairlineDividerColor = OutlineVariant.copy(alpha = 0.40f)

// Success green for OTP copied state
val CopiedGreenBg = Color(0xFFC4EED0)
val CopiedGreenFg = Color(0xFF074E18)

// Avatar pastel pairs (deterministic per sender, hash sender+recipient, 6 colors)
data class AvatarColorPair(val background: Color, val foreground: Color)

val AvatarColors = listOf(
    AvatarColorPair(Color(0xFFEADDFF), Color(0xFF21005D)), // Lavender
    AvatarColorPair(Color(0xFFFFD8E4), Color(0xFF31111D)), // Rose
    AvatarColorPair(Color(0xFFD3EEDB), Color(0xFF0A3818)), // Sage
    AvatarColorPair(Color(0xFFD3E3FD), Color(0xFF041E49)), // Sky
    AvatarColorPair(Color(0xFFFFDEA6), Color(0xFF2A1800)), // Amber
    AvatarColorPair(Color(0xFFFFDBD0), Color(0xFF3B0900))  // Coral
)

fun getAvatarColorPair(sender: String, recipient: String = ""): AvatarColorPair {
    val key = "${sender.trim().lowercase()}:${recipient.trim().lowercase()}"
    val hash = abs(key.hashCode())
    return AvatarColors[hash % AvatarColors.size]
}

// M3 Light ColorScheme
val ShardMailColorScheme: ColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    scrim = Scrim,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    inversePrimary = InversePrimary,
    surfaceDim = SurfaceDim,
    surfaceBright = SurfaceBright,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest
)

// PART 0 — TYPOGRAPHY TOKENS (M3 TYPE SCALE — NO CUSTOM SIZES)
val ShardMailTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal, // 400
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold, // 600
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold, // 600
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal, // 400
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal, // 400 (subject)
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal, // 400 (preview)
        fontSize = 13.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold, // 600 (buttons)
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold, // 600 (chips, meta)
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold, // 600 (section headers, uppercase)
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.08.sp * 11 // +0.08em tracking
    )
)

// PART 0 — SHAPE TOKENS (M3 EXPRESSIVE)
val ShardMailShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),          // Filter chips
    medium = RoundedCornerShape(12.dp),        // OTP chip, snackbar
    large = RoundedCornerShape(16.dp),         // FAB, text fields
    extraLarge = RoundedCornerShape(28.dp)     // Bottom sheet top corners, dialog cards
)

val FullShape = RoundedCornerShape(999.dp)     // Search pill, buttons, nav indicator, avatar circle
val AccountCardShape = RoundedCornerShape(20.dp) // Account card 20dp radius
val RecipientPillShape = RoundedCornerShape(6.dp) // Short recipient pill 6dp radius

// PART 0 — MOTION TOKENS (M3 EXPRESSIVE)
val M3Easing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
const val MotionTapDuration = 180
const val MotionSheetDuration = 280
const val MotionDetailDuration = 280
const val PressScaleFactor = 0.98f

@Composable
fun ShardMailTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ShardMailColorScheme,
        typography = ShardMailTypography,
        shapes = ShardMailShapes,
        content = content
    )
}
