package com.appmaze.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appmaze.usage.DistractionLevel

/**
 * A single app icon cell in the launcher grid.
 * Engineered for 120Hz high-refresh-rate rendering:
 *  - Uses graphicsLayer for press-scale (zero recomposition/relayout overhead)
 *  - Uses clickable with touch slop so scroll flings never trigger press animation
 *  - Distraction dot drawn directly via drawBehind on Canvas
 *  - Minimal composition tree with zero redundant clip layers
 *
 * Supports icon disguise: when [isDisguised] is true and [disguiseIcon] is non-null,
 * the donor icon is shown instead of the real icon. A subtle mask indicator appears
 * at bottom-left to hint at the disguise. Accessibility always uses the real [appName].
 */
@Composable
fun AppGridItem(
    appName: String,
    icon: ImageBitmap?,
    showLabel: Boolean = true,
    isDistracting: Boolean = false,
    distractionLevel: DistractionLevel = DistractionLevel.LOW,
    isDisguised: Boolean = false,
    disguiseIcon: ImageBitmap? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val errorColor = MaterialTheme.colorScheme.error
    val warningColor = MaterialTheme.colorScheme.tertiary
    val disguiseTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)

    // Press detection with proper platform touch-slop handling.
    // Scrolling will NOT activate press state, ensuring zero animation overhead during 120Hz flings.
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 800f),
        label = "press_scale"
    )

    // Distraction dot color based on level
    val dotColor = remember(distractionLevel, errorColor, warningColor) {
        when (distractionLevel) {
            DistractionLevel.EXTREME -> errorColor
            DistractionLevel.HIGH -> errorColor.copy(alpha = 0.8f)
            DistractionLevel.MEDIUM -> warningColor
            DistractionLevel.LOW -> warningColor.copy(alpha = 0.5f)
        }
    }

    // Choose which icon to display
    val displayIcon = if (isDisguised && disguiseIcon != null) disguiseIcon else icon

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .semantics {
                // Accessibility always identifies the REAL app, never the disguise
                contentDescription = "App: $appName${if (isDistracting) ", marked as distracting" else ""}"
            }
    ) {
        // Icon with optional distraction indicator and disguise mask
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .then(
                    if (isDistracting) {
                        Modifier.drawBehind {
                            val dotRadius = 5.dp.toPx()
                            drawCircle(
                                color = dotColor,
                                radius = dotRadius,
                                center = Offset(size.width - dotRadius, dotRadius)
                            )
                        }
                    } else Modifier
                )
        ) {
            if (displayIcon != null) {
                Image(
                    bitmap = displayIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                // Lightweight placeholder while icon loads
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = appName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }

            // Subtle disguise mask indicator at bottom-left
            if (isDisguised) {
                Text(
                    text = "🎭",
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 1.dp, bottom = 1.dp)
                        .graphicsLayer { alpha = 0.5f }
                )
            }
        }

        // Label
        if (showLabel) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = appName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 72.dp)
            )
        }
    }
}
