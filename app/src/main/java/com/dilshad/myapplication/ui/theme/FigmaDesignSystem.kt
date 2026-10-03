package com.dilshad.myapplication.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pixel-perfect design tokens matching the Figma Make prototype:
 * (C:\Users\LENOVO\Downloads\Build prototype\src\product.css)
 */
object FigmaTheme {
    val Ink = Color(0xFF171714)             // --p-ink: #171714 / #181713
    val Paper = Color(0xFFF3F0E7)           // --p-paper: #f3f0e7 / #f4f0e7
    val White = Color(0xFFFFFDF8)           // --p-white: #fffdf8
    val Orange = Color(0xFFEE5B2B)          // --p-orange: #ee5b2b / #f05a24
    val OrangeTint = Color(0xFFF3DFD3)      // Warm tinted concept box background
    val Mint = Color(0xFFD6E6E2)            // Mint book cover / badge
    val Yellow = Color(0xFFF2D25F)          // Yellow book cover / highlight
    val Purple = Color(0xFFE2D9EF)          // Subject purple accent
    val Salmon = Color(0xFFF3CDBB)          // Error / weak topic background
    val Muted = Color(0xFF6D695F)           // --p-muted: #6d695f
    val Green = Color(0xFF16864E)           // Offline / Ready green dot
    val Hairline = Color(0x33171714)        // Hairline divider: rgba(24, 23, 19, 0.2)
    val Watermark = Color(0x1A171714)       // Giant watermark number color: #e5e1d7 / rgba(23,23,20,.1)
    val WatermarkLight = Color(0xFFE5E1D7)  // Giant watermark on white card

    val LabelStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp,
        letterSpacing = 1.2.sp,
        color = Ink
    )

    val HeadlineHero = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        letterSpacing = (-1.5).sp,
        lineHeight = 38.sp,
        color = Ink
    )

    val HeadlineCompact = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = (-1.0).sp,
        lineHeight = 26.sp,
        color = Ink
    )

    val SerifBody = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = Ink
    )
}

/**
 * Brutalist card container with a solid, hard-offset rectangular drop shadow.
 * Pixel-identical to CSS `border: 2px solid var(--p-ink); box-shadow: 8px 8px 0 var(--p-ink);`
 */
@Composable
fun BrutalistCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = FigmaTheme.White,
    borderColor: Color = FigmaTheme.Ink,
    borderWidth: Dp = 2.dp,
    shadowOffset: Dp = 6.dp,
    shadowColor: Color = FigmaTheme.Ink,
    shape: Shape = RectangleShape,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.padding(bottom = shadowOffset, end = shadowOffset)
    ) {
        // Solid drop shadow block behind
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(shadowColor, shape)
        )
        // Content container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor, shape)
                .border(borderWidth, borderColor, shape)
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
            content = content
        )
    }
}

/**
 * Brutalist tactile action button.
 * Pixel-identical to CSS `.product-button.orange { background: var(--p-orange); box-shadow: 6px 6px 0 var(--p-ink); }`
 */
@Composable
fun BrutalistButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = FigmaTheme.Orange,
    textColor: Color = FigmaTheme.Ink,
    borderColor: Color = FigmaTheme.Ink,
    borderWidth: Dp = 2.dp,
    shadowOffset: Dp = 5.dp,
    shadowColor: Color = FigmaTheme.Ink,
    showArrow: Boolean = true,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    minHeight: Dp = 46.dp,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val effectiveBg = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.4f)
    val effectiveText = if (enabled) textColor else textColor.copy(alpha = 0.5f)

    Box(
        modifier = modifier.padding(bottom = shadowOffset, end = shadowOffset)
    ) {
        if (shadowOffset > 0.dp && enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = shadowOffset, y = shadowOffset)
                    .background(shadowColor)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .background(effectiveBg)
                .border(borderWidth, borderColor)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(contentPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                }
                Text(
                    text = text.uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    letterSpacing = 0.5.sp,
                    color = effectiveText,
                    maxLines = 1
                )
            }
            if (showArrow) {
                Text(
                    text = "→",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = effectiveText
                )
            }
        }
    }
}

/**
 * Editorial label kicker.
 * Pixel-identical to `.product-label` / `.kicker`
 */
@Composable
fun FigmaLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = FigmaTheme.Ink
) {
    Text(
        text = text.uppercase(),
        style = FigmaTheme.LabelStyle.copy(color = color),
        modifier = modifier
    )
}

/**
 * Online / Offline status badge.
 * Pixel-identical to `.ready-label` with green circular indicator.
 */
@Composable
fun FigmaReadyLabel(
    text: String = "LOCAL LIBRARY",
    online: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(if (online) FigmaTheme.Green else FigmaTheme.Muted, RoundedCornerShape(50))
        )
        Text(
            text = text.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 0.8.sp,
            color = if (online) FigmaTheme.Green else FigmaTheme.Muted
        )
    }
}

/**
 * Editorial page header matching `.product-page-head`
 */
@Composable
fun FigmaPageHead(
    label: String,
    title: String,
    copy: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Orange top accent bar
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(4.dp)
                .background(FigmaTheme.Orange)
        )
        FigmaLabel(label)
        Text(
            text = title.uppercase(),
            style = FigmaTheme.HeadlineHero
        )
        if (!copy.isNullOrBlank()) {
            Text(
                text = copy,
                fontFamily = FontFamily.Serif,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = FigmaTheme.Muted
            )
        }
    }
}

/**
 * Brutalist filter chip with black border and offset/solid style
 */
@Composable
fun BrutalistChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(if (selected) FigmaTheme.Ink else FigmaTheme.White)
            .border(1.5.dp, FigmaTheme.Ink)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = text.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 0.8.sp,
            color = if (selected) FigmaTheme.White else FigmaTheme.Ink
        )
    }
}
