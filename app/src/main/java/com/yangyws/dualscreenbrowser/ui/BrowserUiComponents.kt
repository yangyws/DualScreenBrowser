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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Color
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
        contentColor = Color.White,
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
            tint = Color.Gray.copy(alpha = 0.7f),
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            color = Color.Gray,
            fontSize = 13.sp
        )
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
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color(0xFF1E222B)
        ) {
            menuContent { expanded = false }
        }
    }
}

/** Draggable floating menu button with isolated recompose scope for the overflow menu. */
@Composable
fun FloatingMenuButton(
    menuContent: @Composable (dismiss: () -> Unit) -> Unit
) {
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
                Surface(
                    onClick = { expanded = true },
                    shape = CircleShape,
                    color = if (expanded) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                    } else {
                        Color(0xD9161B24)
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (expanded) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.White.copy(alpha = 0.22f)
                        }
                    ),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(46.dp)
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
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                if (expanded) {
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
                    Popup(
                        popupPositionProvider = positionProvider,
                        onDismissRequest = { expanded = false },
                        properties = PopupProperties(focusable = true)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E222B),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                            shadowElevation = 10.dp,
                            modifier = Modifier.heightIn(max = maxMenuHeight)
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(IntrinsicSize.Max)
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp)
                            ) {
                                menuContent { expanded = false }
                            }
                        }
                    }
                }
            }
        }
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
            enter = fadeIn(tween(160, easing = StrongEaseOut)),
            exit = fadeOut(tween(130, easing = StrongEaseOut))
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
            enter = slideInHorizontally(tween(200, easing = StrongEaseOut)) { it } +
                fadeIn(tween(160, easing = StrongEaseOut)),
            exit = slideOutHorizontally(tween(160, easing = StrongEaseOut)) { it } +
                fadeOut(tween(120, easing = StrongEaseOut)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                color = Color(0xFF171A21),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
                modifier = Modifier
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
                            color = Color.White,
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
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(Modifier.height(10.dp))
                    content()
                }
            }
        }
    }
}
