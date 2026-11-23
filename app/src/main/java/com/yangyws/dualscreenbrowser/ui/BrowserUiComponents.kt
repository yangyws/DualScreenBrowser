package com.yangyws.dualscreenbrowser.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

/** Strong ease-out curve matching design-engineering motion tokens. */
val StrongEaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

@Composable
fun TactileIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
    size: Dp = 36.dp,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.90f else 1f,
        animationSpec = tween(durationMillis = 110, easing = StrongEaseOut),
        label = "tactileScale"
    )

    val containerColor = when {
        !enabled -> Color.Transparent
        active -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        else -> Color.Transparent
    }
    val border = if (active && enabled) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.38f))
    } else {
        null
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = border,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.38f
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
fun StatusChip(
    label: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = accentColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.32f)),
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
    }
}

@Composable
fun CompactMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp)
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurface,
            androidx.compose.material3.LocalTextStyle provides androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        ) {
            if (leadingIcon != null) {
                Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                    leadingIcon()
                }
                Spacer(Modifier.width(10.dp))
            }
            Box(modifier = Modifier.weight(1f)) {
                text()
            }
            if (trailingIcon != null) {
                Spacer(Modifier.width(8.dp))
                trailingIcon()
            }
        }
    }
}

/** Isolates overflow menu open/close state into its own recompose scope so opening ⋮ is instant. */
@Composable
fun OverflowMenuButton(
    menuContent: @Composable (dismiss: () -> Unit) -> Unit
) {
    var expanded by remember { androidx.compose.runtime.mutableStateOf(false) }
    Box {
        TactileIconButton(
            onClick = { expanded = true },
            active = expanded
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            menuContent { expanded = false }
        }
    }
}

/** Draggable floating menu button with isolated recompose scope for the overflow menu. */
@Composable
fun FloatingMenuButton(
    isRainbow: Boolean = false,
    extraPopupExpanded: Boolean = false,
    onDismissExtraPopup: () -> Unit = {},
    extraPopupContent: (@Composable () -> Unit)? = null,
    menuContent: @Composable (dismiss: () -> Unit) -> Unit
) {
    val rainbowBrush = remember {
        Brush.sweepGradient(
            listOf(
                Color(0xFF3B82F6), // X Blue
                Color(0xFFEAB308), // B Yellow
                Color(0xFF22C55E), // Y Green
                Color(0xFFEF4444), // A Red
                Color(0xFF3B82F6)
            )
        )
    }
    val rainbowBarBrush = remember {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF3B82F6),
                Color(0xFFEAB308),
                Color(0xFF22C55E),
                Color(0xFFEF4444)
            )
        )
    }
    var expanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val btnSizePx = with(density) { 46.dp.toPx() }
    val marginPx = with(density) { 14.dp.toPx() }
    var parentWidth by remember { mutableFloatStateOf(0f) }
    var parentHeight by remember { mutableFloatStateOf(0f) }
    var offsetX by remember { mutableFloatStateOf(-1f) }
    var offsetY by remember { mutableFloatStateOf(-1f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                val w = size.width.toFloat()
                val h = size.height.toFloat()
                parentWidth = w
                parentHeight = h
                if (w > 0f && h > 0f && (offsetX < 0f || offsetY < 0f)) {
                    offsetX = (w - btnSizePx - marginPx).coerceAtLeast(marginPx)
                    offsetY = (h - btnSizePx - marginPx).coerceAtLeast(marginPx)
                } else if (w > 0f && h > 0f) {
                    offsetX = offsetX.coerceIn(marginPx, (w - btnSizePx - marginPx).coerceAtLeast(marginPx))
                    offsetY = offsetY.coerceIn(marginPx, (h - btnSizePx - marginPx).coerceAtLeast(marginPx))
                }
            }
    ) {
        if (offsetX >= 0f && offsetY >= 0f) {
            Box(
                modifier = Modifier.offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            ) {
                val buttonInteraction = remember { MutableInteractionSource() }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (expanded) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                            } else {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                            }
                        )
                        .border(
                            border = if (isRainbow) {
                                BorderStroke(1.8.dp, rainbowBrush)
                            } else {
                                BorderStroke(
                                    width = 1.dp,
                                    color = if (expanded) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
                                    }
                                )
                            },
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = buttonInteraction,
                            indication = null,
                            onClick = { expanded = true }
                        )
                        .pointerInput(parentWidth, parentHeight) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val maxX = (parentWidth - btnSizePx - marginPx).coerceAtLeast(marginPx)
                                val maxY = (parentHeight - btnSizePx - marginPx).coerceAtLeast(marginPx)
                                offsetX = (offsetX + dragAmount.x).coerceIn(marginPx, maxX)
                                offsetY = (offsetY + dragAmount.y).coerceIn(marginPx, maxY)
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                if (expanded || (extraPopupExpanded && extraPopupContent != null)) {
                    val gapPx = with(density) { 8.dp.roundToPx() }
                    val edgeMarginPx = with(density) { 8.dp.roundToPx() }
                    val openAbove = (offsetY + btnSizePx / 2f) > (parentHeight / 2f)
                    val maxMenuHeight = with(density) {
                        val available = if (openAbove) {
                            offsetY - marginPx - 8.dp.toPx()
                        } else {
                            parentHeight - (offsetY + btnSizePx) - marginPx - 8.dp.toPx()
                        }
                        available.coerceAtLeast(160.dp.toPx()).toDp()
                    }
                    val positionProvider = remember(openAbove, gapPx, edgeMarginPx) {
                        object : PopupPositionProvider {
                            override fun calculatePosition(
                                anchorBounds: IntRect,
                                windowSize: IntSize,
                                layoutDirection: LayoutDirection,
                                popupContentSize: IntSize
                            ): IntOffset {
                                val alignRight = anchorBounds.center.x > windowSize.width / 2
                                val rawX = if (alignRight) {
                                    anchorBounds.right - popupContentSize.width
                                } else {
                                    anchorBounds.left
                                }
                                val x = rawX.coerceIn(
                                    edgeMarginPx,
                                    (windowSize.width - popupContentSize.width - edgeMarginPx).coerceAtLeast(edgeMarginPx)
                                )
                                val rawY = if (openAbove) {
                                    anchorBounds.top - popupContentSize.height - gapPx
                                } else {
                                    anchorBounds.bottom + gapPx
                                }
                                val y = rawY.coerceIn(
                                    edgeMarginPx,
                                    (windowSize.height - popupContentSize.height - edgeMarginPx).coerceAtLeast(edgeMarginPx)
                                )
                                return IntOffset(x, y)
                            }
                        }
                    }
                    if (expanded) {
                        Popup(
                            popupPositionProvider = positionProvider,
                            onDismissRequest = { expanded = false },
                            properties = PopupProperties(focusable = true)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = if (isRainbow) {
                                    BorderStroke(1.5.dp, rainbowBarBrush)
                                } else {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f))
                                },
                                shadowElevation = 0.dp,
                                modifier = Modifier.heightIn(max = maxMenuHeight)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .width(304.dp)
                                        .verticalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp)
                                ) {
                                    menuContent { expanded = false }
                                }
                            }
                        }
                    } else if (extraPopupExpanded && extraPopupContent != null) {
                        Popup(
                            popupPositionProvider = positionProvider,
                            onDismissRequest = onDismissExtraPopup,
                            properties = PopupProperties(focusable = true)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = if (isRainbow) {
                                    BorderStroke(1.5.dp, rainbowBarBrush)
                                } else {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f))
                                },
                                shadowElevation = 0.dp,
                                modifier = Modifier.heightIn(max = maxMenuHeight)
                            ) {
                                extraPopupContent()
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Compact clamshell AYN Thor handheld icon tinted for each ThorTheme option. */
@Composable
fun MiniThorThemeIcon(
    shellColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val lidW = w * 0.88f
        val lidH = h * 0.40f
        val lidX = (w - lidW) / 2f
        drawRoundRect(
            color = shellColor,
            topLeft = Offset(lidX, h * 0.04f),
            size = Size(lidW, lidH),
            cornerRadius = CornerRadius(w * 0.1f, w * 0.1f)
        )
        drawRoundRect(
            color = Color(0xFF0E131B),
            topLeft = Offset(lidX + lidW * 0.09f, h * 0.08f),
            size = Size(lidW * 0.82f, lidH * 0.78f),
            cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
        )
        drawRoundRect(
            color = accentColor,
            topLeft = Offset(w * 0.34f, h * 0.45f),
            size = Size(w * 0.32f, h * 0.07f),
            cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
        )
        val botW = w * 0.92f
        val botH = h * 0.42f
        val botX = (w - botW) / 2f
        val botY = h * 0.53f
        drawRoundRect(
            color = shellColor,
            topLeft = Offset(botX, botY),
            size = Size(botW, botH),
            cornerRadius = CornerRadius(w * 0.1f, w * 0.1f)
        )
        drawRoundRect(
            color = Color(0xFF0E131B),
            topLeft = Offset(w * 0.25f, botY + botH * 0.12f),
            size = Size(w * 0.50f, botH * 0.72f),
            cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
        )
        drawCircle(
            color = accentColor,
            radius = w * 0.055f,
            center = Offset(botX + botW * 0.12f, botY + botH * 0.46f)
        )
        drawCircle(
            color = accentColor,
            radius = w * 0.055f,
            center = Offset(botX + botW * 0.88f, botY + botH * 0.46f)
        )
        drawRoundRect(
            color = accentColor.copy(alpha = 0.5f),
            topLeft = Offset(lidX, h * 0.04f),
            size = Size(lidW, lidH),
            cornerRadius = CornerRadius(w * 0.1f, w * 0.1f),
            style = Stroke(width = 1.5f)
        )
    }
}

@Composable
fun SidePanelSheet(
    visible: Boolean,
    title: String,
    onDismiss: () -> Unit,
    dimBackground: Boolean = true,
    compactWidth: Boolean = false,
    headerActions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(120, easing = StrongEaseOut)),
            exit = fadeOut(tween(80, easing = StrongEaseOut))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (dimBackground) Color.Black.copy(alpha = 0.52f) else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(tween(150, easing = StrongEaseOut)) { it } +
                fadeIn(tween(120, easing = StrongEaseOut)),
            exit = slideOutHorizontally(tween(110, easing = StrongEaseOut)) { it } +
                fadeOut(tween(80, easing = StrongEaseOut)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)),
                modifier = Modifier
                    .graphicsLayer {}
                    .fillMaxHeight()
                    .fillMaxWidth(if (compactWidth) 0.44f else 0.58f)
                    .widthIn(min = if (compactWidth) 260.dp else 300.dp, max = if (compactWidth) 360.dp else 460.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            headerActions()
                            TactileIconButton(onClick = onDismiss, size = 32.dp) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f))
                    Spacer(Modifier.height(10.dp))
                    content()
                }
            }
        }
    }
}

/** Full-spectrum 360-degree rainbow hue slider with live color preview thumb. */
@Composable
fun HueColorSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val spectrumColors = remember {
        listOf(
            Color.hsv(0f, 0.72f, 0.94f),
            Color.hsv(60f, 0.72f, 0.94f),
            Color.hsv(120f, 0.72f, 0.94f),
            Color.hsv(180f, 0.72f, 0.94f),
            Color.hsv(240f, 0.72f, 0.94f),
            Color.hsv(300f, 0.72f, 0.94f),
            Color.hsv(360f, 0.72f, 0.94f)
        )
    }
    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current
    val thumbSize = 22.dp
    val thumbPx = with(density) { thumbSize.toPx() }
    val safeHue = hue.coerceIn(0f, 360f)
    val thumbColor = Color.hsv(safeHue, 0.65f, 0.92f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    val ratio = (pos.x / trackWidthPx).coerceIn(0f, 1f)
                    onHueChange(ratio * 360f)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val ratio = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                    onHueChange(ratio * 360f)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(spectrumColors))
        )
        val maxOffset = (trackWidthPx - thumbPx).coerceAtLeast(0f)
        val thumbOffsetX = ((safeHue / 360f) * maxOffset).roundToInt()
        Surface(
            shape = CircleShape,
            color = thumbColor,
            border = BorderStroke(2.5.dp, Color.White),
            shadowElevation = 4.dp,
            modifier = Modifier
                .offset { IntOffset(thumbOffsetX, 0) }
                .size(thumbSize)
        ) {}
    }
}
