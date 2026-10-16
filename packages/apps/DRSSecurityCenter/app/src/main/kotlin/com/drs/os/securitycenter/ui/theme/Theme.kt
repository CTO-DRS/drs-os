/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS theme: maps branding/colors.md tokens to a Material 3 scheme.
 * Both themes ship at parity (colors.md usage rule 5).
 */
package com.drs.os.securitycenter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * Type ramp from branding/typography.md (sp, both locales; weight axis
 * limited to 400/500/600). Font binaries are NOT bundled yet — the
 * inclusion checklist in typography.md (license text, provenance,
 * specimen review) runs before any font file enters the tree. The
 * platform default font chain renders until then; that is an honest,
 * documented interim state, not a silent substitution.
 */
private val DrsTypography = Typography(
    headlineMedium = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal)
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceAlt,
    onSurfaceVariant = DarkTextSecondary,
    error = DarkDanger,
    outline = DarkTextDisabled
)

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceAlt,
    onSurfaceVariant = LightTextSecondary,
    error = LightDanger,
    outline = LightTextDisabled
)

@Composable
fun DrsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = DrsTypography,
        content = content
    )
}
