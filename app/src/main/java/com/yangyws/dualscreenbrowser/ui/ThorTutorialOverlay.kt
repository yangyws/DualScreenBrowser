package com.yangyws.dualscreenbrowser.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangyws.dualscreenbrowser.R
import com.yangyws.dualscreenbrowser.browser.ThorTheme

private data class TutorialStep(
    val titleRes: Int,
    val descRes: Int
)

@Composable
fun ThorTutorialOverlay(
    visible: Boolean,
    theme: ThorTheme = ThorTheme.BLACK,
    customPrimaryColor: Color = Color(theme.primaryColor),
    onFinish: () -> Unit
) {
    val steps = remember {
        listOf(
            TutorialStep(R.string.tutorial_step1_title, R.string.tutorial_step1_desc),
            TutorialStep(R.string.tutorial_step2_title, R.string.tutorial_step2_desc),
            TutorialStep(R.string.tutorial_step3_title, R.string.tutorial_step3_desc),
            TutorialStep(R.string.tutorial_step4_title, R.string.tutorial_step4_desc)
        )
    }
    var stepIndex by remember(visible) { mutableIntStateOf(0) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180, easing = StrongEaseOut)),
        exit = fadeOut(tween(140, easing = StrongEaseOut))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = Color(theme.surfaceColor),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .widthIn(max = 580.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Custom AYN Thor clamshell dual-screen diagram
                    ThorHandheldDiagram(
                        highlightStep = stepIndex,
                        theme = theme,
                        accent = customPrimaryColor,
                        modifier = Modifier
                            .width(190.dp)
                            .height(210.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                steps.indices.forEach { i ->
                                    Box(
                                        modifier = Modifier
                                            .size(if (i == stepIndex) 16.dp else 6.dp, 6.dp)
                                            .background(
                                                color = if (i == stepIndex) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    Color.White.copy(alpha = 0.25f)
                                                },
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                            TextButton(onClick = onFinish) {
                                Text(stringResource(R.string.tutorial_skip), color = Color.Gray, fontSize = 12.sp)
                            }
                        }

                        AnimatedContent(
                            targetState = stepIndex,
                            transitionSpec = {
                                fadeIn(tween(160, easing = StrongEaseOut)) togetherWith
                                    fadeOut(tween(120, easing = StrongEaseOut))
                            },
                            label = "tutorialStep"
                        ) { idx ->
                            val step = steps[idx]
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = stringResource(step.titleRes),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = stringResource(step.descRes),
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    color = Color(0xFFB8C0D0)
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (stepIndex > 0) {
                                OutlinedButton(
                                    onClick = { stepIndex-- },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.tutorial_prev))
                                }
                            }
                            Button(
                                onClick = {
                                    if (stepIndex < steps.lastIndex) {
                                        stepIndex++
                                    } else {
                                        onFinish()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    stringResource(
                                        if (stepIndex < steps.lastIndex) R.string.tutorial_next else R.string.tutorial_done
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Vector Canvas illustration of the AYN Thor clamshell dual-screen handheld:
 * 16:9 top lid, center hinge, 8:7 bottom screen + touchpad, left stick/D-Pad,
 * right ABXY/stick, Select/Start, and L1/L2/R1/R2 shoulder triggers.
 */
@Composable
internal fun ThorHandheldDiagram(
    highlightStep: Int,
    theme: ThorTheme,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val isLightShell = theme == ThorTheme.WHITE || theme == ThorTheme.RAINBOW
    val shellFill = Color(theme.shellColor)
    val shellStroke = if (isLightShell) Color(0xFF9BA4B8) else Color(0xFF4A5368)
    val screenBg = Color(0xFF12151C)
    val dimControl = if (isLightShell) Color(0xFF7D869B) else Color(0xFF5D667B)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Top L1/L2 & R1/R2 shoulder triggers
        val shoulderColor = if (highlightStep == 2) accent else dimControl
        drawRoundRect(
            color = shoulderColor,
            topLeft = Offset(w * 0.14f, h * 0.02f),
            size = Size(w * 0.18f, h * 0.035f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawRoundRect(
            color = shoulderColor,
            topLeft = Offset(w * 0.68f, h * 0.02f),
            size = Size(w * 0.18f, h * 0.035f),
            cornerRadius = CornerRadius(6f, 6f)
        )

        // Top Lid (16:9 screen housing)
        val lidLeft = w * 0.08f
        val lidTop = h * 0.06f
        val lidW = w * 0.84f
        val lidH = h * 0.39f
        drawRoundRect(
            color = shellFill,
            topLeft = Offset(lidLeft, lidTop),
            size = Size(lidW, lidH),
            cornerRadius = CornerRadius(14f, 14f)
        )
        drawRoundRect(
            color = if (highlightStep == 0 || highlightStep == 3) accent else shellStroke,
            topLeft = Offset(lidLeft, lidTop),
            size = Size(lidW, lidH),
            cornerRadius = CornerRadius(14f, 14f),
            style = Stroke(width = 2.5f)
        )

        // Top 16:9 Screen
        val topScrLeft = w * 0.13f
        val topScrTop = h * 0.10f
        val topScrW = w * 0.74f
        val topScrH = h * 0.31f
        drawRoundRect(
            color = screenBg,
            topLeft = Offset(topScrLeft, topScrTop),
            size = Size(topScrW, topScrH),
            cornerRadius = CornerRadius(8f, 8f)
        )
        // Top browser chrome bar highlight (Step 3)
        drawRoundRect(
            color = if (highlightStep == 3) accent else accent.copy(alpha = 0.35f),
            topLeft = Offset(topScrLeft, topScrTop),
            size = Size(topScrW, topScrH * 0.18f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        // Simulated page lines inside top screen
        val lineColor = if (highlightStep == 0) accent.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.28f)
        drawLine(lineColor, Offset(topScrLeft + 14f, topScrTop + topScrH * 0.42f), Offset(topScrLeft + topScrW * 0.72f, topScrTop + topScrH * 0.42f), strokeWidth = 4f)
        drawLine(lineColor, Offset(topScrLeft + 14f, topScrTop + topScrH * 0.62f), Offset(topScrLeft + topScrW * 0.54f, topScrTop + topScrH * 0.62f), strokeWidth = 4f)
        drawLine(lineColor, Offset(topScrLeft + 14f, topScrTop + topScrH * 0.82f), Offset(topScrLeft + topScrW * 0.80f, topScrTop + topScrH * 0.82f), strokeWidth = 4f)

        // Clamshell Hinge
        val hingeTop = h * 0.46f
        val hingeH = h * 0.045f
        drawRoundRect(
            color = if (highlightStep == 0) accent else shellStroke,
            topLeft = Offset(w * 0.14f, hingeTop),
            size = Size(w * 0.72f, hingeH),
            cornerRadius = CornerRadius(8f, 8f)
        )

        // Bottom Deck (controller + 8:7 bottom screen)
        val deckLeft = w * 0.04f
        val deckTop = h * 0.515f
        val deckW = w * 0.92f
        val deckH = h * 0.45f
        drawRoundRect(
            color = shellFill,
            topLeft = Offset(deckLeft, deckTop),
            size = Size(deckW, deckH),
            cornerRadius = CornerRadius(18f, 18f)
        )
        drawRoundRect(
            color = shellStroke,
            topLeft = Offset(deckLeft, deckTop),
            size = Size(deckW, deckH),
            cornerRadius = CornerRadius(18f, 18f),
            style = Stroke(width = 2.5f)
        )

        // Center 8:7 Bottom Screen
        val botScrLeft = w * 0.30f
        val botScrTop = h * 0.55f
        val botScrW = w * 0.40f
        val botScrH = h * 0.34f
        drawRoundRect(
            color = screenBg,
            topLeft = Offset(botScrLeft, botScrTop),
            size = Size(botScrW, botScrH),
            cornerRadius = CornerRadius(8f, 8f)
        )
        drawRoundRect(
            color = if (highlightStep == 0) accent else shellStroke.copy(alpha = 0.7f),
            topLeft = Offset(botScrLeft, botScrTop),
            size = Size(botScrW, botScrH),
            cornerRadius = CornerRadius(8f, 8f),
            style = Stroke(width = 2f)
        )
        // Bottom content lines (Step 0) & Touchpad zone (Step 1)
        drawLine(lineColor, Offset(botScrLeft + 10f, botScrTop + botScrH * 0.22f), Offset(botScrLeft + botScrW * 0.78f, botScrTop + botScrH * 0.22f), strokeWidth = 4f)
        drawLine(lineColor, Offset(botScrLeft + 10f, botScrTop + botScrH * 0.42f), Offset(botScrLeft + botScrW * 0.62f, botScrTop + botScrH * 0.42f), strokeWidth = 4f)

        val padColor = if (highlightStep == 1) accent.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.1f)
        drawRoundRect(
            color = padColor,
            topLeft = Offset(botScrLeft + 3f, botScrTop + botScrH * 0.62f),
            size = Size(botScrW - 6f, botScrH * 0.34f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        if (highlightStep == 1) {
            drawRoundRect(
                color = accent,
                topLeft = Offset(botScrLeft + 3f, botScrTop + botScrH * 0.62f),
                size = Size(botScrW - 6f, botScrH * 0.34f),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 2f)
            )
        }

        // Left Controls: Analog Stick + D-Pad + Select
        val padHighlight = if (highlightStep == 2) accent else dimControl
        drawCircle(padHighlight, radius = w * 0.055f, center = Offset(w * 0.16f, h * 0.63f))
        // D-Pad cross
        val dpCx = w * 0.16f
        val dpCy = h * 0.78f
        val dpArm = w * 0.030f
        val dpThick = w * 0.022f
        drawRect(padHighlight, topLeft = Offset(dpCx - dpThick / 2f, dpCy - dpArm), size = Size(dpThick, dpArm * 2f))
        drawRect(padHighlight, topLeft = Offset(dpCx - dpArm, dpCy - dpThick / 2f), size = Size(dpArm * 2f, dpThick))
        // Select pill
        drawRoundRect(padHighlight, topLeft = Offset(w * 0.19f, h * 0.89f), size = Size(w * 0.036f, h * 0.015f), cornerRadius = CornerRadius(3f, 3f))

        // Right Controls: ABXY + Right Stick + Start
        val abxyCx = w * 0.84f
        val abxyCy = h * 0.63f
        val btnR = w * 0.024f
        val abxyOffset = w * 0.028f
        if (theme == ThorTheme.RAINBOW) {
            drawCircle(Color(0xFF3B82F6), radius = btnR, center = Offset(abxyCx, abxyCy - abxyOffset)) // X Blue
            drawCircle(Color(0xFFEAB308), radius = btnR, center = Offset(abxyCx, abxyCy + abxyOffset)) // B Yellow
            drawCircle(Color(0xFF22C55E), radius = btnR, center = Offset(abxyCx - abxyOffset, abxyCy)) // Y Green
            drawCircle(Color(0xFFEF4444), radius = btnR, center = Offset(abxyCx + abxyOffset, abxyCy)) // A Red
        } else {
            drawCircle(padHighlight, radius = btnR, center = Offset(abxyCx, abxyCy - abxyOffset)) // X
            drawCircle(padHighlight, radius = btnR, center = Offset(abxyCx, abxyCy + abxyOffset)) // B
            drawCircle(padHighlight, radius = btnR, center = Offset(abxyCx - abxyOffset, abxyCy)) // Y
            drawCircle(padHighlight, radius = btnR, center = Offset(abxyCx + abxyOffset, abxyCy)) // A
        }
        // Right Analog Stick
        drawCircle(padHighlight, radius = w * 0.055f, center = Offset(w * 0.84f, h * 0.79f))
        // Start pill
        drawRoundRect(padHighlight, topLeft = Offset(w * 0.73f, h * 0.89f), size = Size(w * 0.036f, h * 0.015f), cornerRadius = CornerRadius(3f, 3f))
    }
}
