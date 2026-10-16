/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS brand color tokens — ONLY values verified in branding/colors.md
 * (WCAG 2.1 ratios computed, 19/19 pairs pass). No ad-hoc hex is
 * allowed in DRS resources (colors.md usage rule 1). The info/light
 * token is deliberately absent: branding/colors.md leaves it
 * unfinalized and unused, and inventing a value here would violate
 * that record.
 */
package com.drs.os.securitycenter.ui.theme

import androidx.compose.ui.graphics.Color

// Dark theme tokens
val DarkBg = Color(0xFF0F1216)
val DarkSurface = Color(0xFF171C23)
val DarkSurfaceAlt = Color(0xFF1E242C)
val DarkTextPrimary = Color(0xFFE8EAED)
val DarkTextSecondary = Color(0xFFA9B4C0)
val DarkTextDisabled = Color(0xFF6B7683)
val DarkPrimary = Color(0xFF2DD4BF)
val DarkOnPrimary = Color(0xFF0F1216)
val DarkDanger = Color(0xFFFF6B6B)
val DarkWarning = Color(0xFFFFB454)
val DarkSuccess = Color(0xFF4ADE80)

// Light theme tokens
val LightBg = Color(0xFFF6F7F9)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceAlt = Color(0xFFEEF1F4)
val LightTextPrimary = Color(0xFF16191D)
val LightTextSecondary = Color(0xFF46505A)
val LightTextDisabled = Color(0xFF7A848E)
val LightPrimary = Color(0xFF0B7A6E)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightDanger = Color(0xFFC53030)
val LightWarning = Color(0xFF8A5300)
val LightSuccess = Color(0xFF1B7A3D)
