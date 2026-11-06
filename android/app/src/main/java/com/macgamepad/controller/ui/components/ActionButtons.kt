package com.macgamepad.controller.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macgamepad.controller.ui.theme.*
import kotlin.math.max

/**
 * Realistic Action Button with console-quality 3D render and zero touch latency.
 */
@Composable
fun ActionButton(
    label: String,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 54.dp,
    color: Color = AccentBlue,
    onPressChanged: (Boolean) -> Unit = {}
) {
    var isPressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .size(buttonSize)
            .pointerInput(buttonSize, onPressChanged) {
                // Immediate low-latency touch handler bypassing drag slop
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.pressed) {
                            change.consume()
                            if (!isPressed) {
                                isPressed = true
                                onPressChanged(true)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        } else {
                            if (isPressed) {
                                isPressed = false
                                onPressChanged(false)
                            }
                        }
                    }
                }
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f

        // Outer glow halo when pressed
        if (isPressed) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.45f), Color.Transparent),
                    center = center,
                    radius = radius * 1.35f
                ),
                radius = radius * 1.35f,
                center = center
            )
        }

        // Drop shadow for 3D depth
        drawCircle(
            color = Color.Black.copy(alpha = 0.4f),
            radius = radius,
            center = Offset(center.x + 2f, center.y + 3f)
        )

        // Glassmorphic / Glossy surface styling
        val surfaceBrush = Brush.verticalGradient(
            colors = if (isPressed) {
                listOf(color.copy(alpha = 0.9f), color.copy(alpha = 0.6f))
            } else {
                listOf(Color(0xFF32323E), Color(0xFF1E1E24))
            }
        )

        drawCircle(
            brush = surfaceBrush,
            radius = radius - 1.5f,
            center = center
        )

        // Metallic / Colored border bezel
        drawCircle(
            color = if (isPressed) Color.White.copy(alpha = 0.8f) else color.copy(alpha = 0.3f),
            radius = radius - 1.5f,
            center = center,
            style = Stroke(width = 2.5f)
        )

        // Subtle specular highlight on top rim (giving glossy look)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent),
                center = Offset(center.x, center.y - radius * 0.4f),
                radius = radius * 0.6f
            ),
            radius = radius * 0.6f,
            center = Offset(center.x, center.y - radius * 0.4f)
        )

        // Label layout
        val textLayout = textMeasurer.measure(
            text = AnnotatedString(label),
            style = TextStyle(
                color = if (isPressed) Color.White else color,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        )
        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(
                center.x - textLayout.size.width / 2f,
                center.y - textLayout.size.height / 2f
            )
        )
    }
}

/**
 * ABXY diamond button cluster.
 */
@Composable
fun ActionButtonCluster(
    modifier: Modifier = Modifier,
    buttonSize: Dp = 52.dp,
    labels: List<String> = listOf("A", "B", "X", "Y"),
    colors: List<Color> = listOf(AccentGreen, AccentRed, AccentBlue, AccentAmber),
    onButtonChanged: (Int, Boolean) -> Unit = { _, _ -> }
) {
    val spacing = buttonSize * 0.15f

    Box(
        modifier = modifier.size(buttonSize * 3 + spacing * 2),
        contentAlignment = Alignment.Center
    ) {
        // Y — top
        ActionButton(
            label = labels[3],
            buttonSize = buttonSize,
            color = colors[3],
            modifier = Modifier.align(Alignment.TopCenter),
            onPressChanged = { onButtonChanged(3, it) }
        )
        // X — left
        ActionButton(
            label = labels[2],
            buttonSize = buttonSize,
            color = colors[2],
            modifier = Modifier.align(Alignment.CenterStart),
            onPressChanged = { onButtonChanged(2, it) }
        )
        // B — right
        ActionButton(
            label = labels[1],
            buttonSize = buttonSize,
            color = colors[1],
            modifier = Modifier.align(Alignment.CenterEnd),
            onPressChanged = { onButtonChanged(1, it) }
        )
        // A — bottom
        ActionButton(
            label = labels[0],
            buttonSize = buttonSize,
            color = colors[0],
            modifier = Modifier.align(Alignment.BottomCenter),
            onPressChanged = { onButtonChanged(0, it) }
        )
    }
}

/**
 * Console-accurate D-Pad.
 */
@Composable
fun DPad(
    modifier: Modifier = Modifier,
    padSize: Dp = 130.dp,
    onDirectionChanged: (up: Boolean, down: Boolean, left: Boolean, right: Boolean) -> Unit = { _, _, _, _ -> }
) {
    var upPressed by remember { mutableStateOf(false) }
    var downPressed by remember { mutableStateOf(false) }
    var leftPressed by remember { mutableStateOf(false) }
    var rightPressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val armWidth = padSize * 0.32f

    Box(
        modifier = modifier.size(padSize),
        contentAlignment = Alignment.Center
    ) {
        // Main DPad cross plate shadow
        Canvas(modifier = Modifier.size(padSize)) {
            drawCircle(
                color = Color.Black.copy(alpha = 0.4f),
                radius = size.minDimension / 2f
            )
        }

        // Up Arm
        DPadArm(
            label = "▲",
            modifier = Modifier.align(Alignment.TopCenter).size(armWidth, padSize * 0.35f),
            onPressChanged = {
                upPressed = it
                if (it) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDirectionChanged(upPressed, downPressed, leftPressed, rightPressed)
            }
        )
        // Down Arm
        DPadArm(
            label = "▼",
            modifier = Modifier.align(Alignment.BottomCenter).size(armWidth, padSize * 0.35f),
            onPressChanged = {
                downPressed = it
                if (it) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDirectionChanged(upPressed, downPressed, leftPressed, rightPressed)
            }
        )
        // Left Arm
        DPadArm(
            label = "◀",
            modifier = Modifier.align(Alignment.CenterStart).size(padSize * 0.35f, armWidth),
            onPressChanged = {
                leftPressed = it
                if (it) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDirectionChanged(upPressed, downPressed, leftPressed, rightPressed)
            }
        )
        // Right Arm
        DPadArm(
            label = "▶",
            modifier = Modifier.align(Alignment.CenterEnd).size(padSize * 0.35f, armWidth),
            onPressChanged = {
                rightPressed = it
                if (it) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDirectionChanged(upPressed, downPressed, leftPressed, rightPressed)
            }
        )

        // Center hub well (glowing bezel)
        Canvas(modifier = Modifier.size(armWidth).align(Alignment.Center)) {
            drawRoundRect(
                color = Color(0xFF101014),
                cornerRadius = CornerRadius(8f, 8f),
                size = size
            )
        }
    }
}

@Composable
private fun DPadArm(
    label: String,
    modifier: Modifier = Modifier,
    onPressChanged: (Boolean) -> Unit = {}
) {
    var isPressed by remember { mutableStateOf(false) }
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .pointerInput(onPressChanged) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.pressed) {
                            change.consume()
                            if (!isPressed) {
                                isPressed = true
                                onPressChanged(true)
                            }
                        } else {
                            if (isPressed) {
                                isPressed = false
                                onPressChanged(false)
                            }
                        }
                    }
                }
            }
    ) {
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = if (isPressed) {
                    listOf(AccentBlue.copy(alpha = 0.9f), AccentBlue.copy(alpha = 0.6f))
                } else {
                    listOf(Color(0xFF282830), Color(0xFF1E1E24))
                }
            ),
            cornerRadius = CornerRadius(12f, 12f),
            size = size
        )
        drawRoundRect(
            color = if (isPressed) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f),
            cornerRadius = CornerRadius(12f, 12f),
            size = size,
            style = Stroke(width = 1.5f)
        )

        val textLayout = textMeasurer.measure(
            text = AnnotatedString(label),
            style = TextStyle(
                color = if (isPressed) Color.White else TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        )
        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(
                (size.width - textLayout.size.width) / 2f,
                (size.height - textLayout.size.height) / 2f
            )
        )
    }
}

/**
 * Wide Rectangular Shoulder Button (L1/R1/L2/R2)
 */
@Composable
fun ShoulderButton(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = AccentBlue,
    onPressChanged: (Boolean) -> Unit = {}
) {
    var isPressed by remember { mutableStateOf(false) }
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .size(75.dp, 36.dp)
            .pointerInput(onPressChanged) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.pressed) {
                            change.consume()
                            if (!isPressed) {
                                isPressed = true
                                onPressChanged(true)
                            }
                        } else {
                            if (isPressed) {
                                isPressed = false
                                onPressChanged(false)
                            }
                        }
                    }
                }
            }
    ) {
        val size = Size(size.width, size.height)

        // Glow
        if (isPressed) {
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.width
                ),
                size = size,
                cornerRadius = CornerRadius(12f, 12f)
            )
        }

        // Drop Shadow
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(1f, 2f),
            size = size,
            cornerRadius = CornerRadius(12f, 12f)
        )

        // Body
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = if (isPressed) {
                    listOf(color.copy(alpha = 0.9f), color.copy(alpha = 0.6f))
                } else {
                    listOf(Color(0xFF2C2C35), Color(0xFF1E1E24))
                }
            ),
            size = size,
            cornerRadius = CornerRadius(12f, 12f)
        )

        // Border Bezel
        drawRoundRect(
            color = if (isPressed) Color.White.copy(alpha = 0.6f) else color.copy(alpha = 0.2f),
            size = size,
            cornerRadius = CornerRadius(12f, 12f),
            style = Stroke(width = 1.5f)
        )

        // Specular highlight
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)
            ),
            size = Size(size.width, size.height * 0.4f),
            cornerRadius = CornerRadius(12f, 12f)
        )

        // Text
        val textLayout = textMeasurer.measure(
            text = AnnotatedString(label),
            style = TextStyle(
                color = if (isPressed) Color.White else color,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )
        )
        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(
                (size.width - textLayout.size.width) / 2f,
                (size.height - textLayout.size.height) / 2f
            )
        )
    }
}

/**
 * Small Oval System Button (Select, Options, Home)
 */
@Composable
fun SystemButton(
    label: String,
    modifier: Modifier = Modifier,
    width: Dp = 60.dp,
    height: Dp = 22.dp,
    onPressChanged: (Boolean) -> Unit = {}
) {
    var isPressed by remember { mutableStateOf(false) }
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .size(width, height)
            .pointerInput(width, height, onPressChanged) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.pressed) {
                            change.consume()
                            if (!isPressed) {
                                isPressed = true
                                onPressChanged(true)
                            }
                        } else {
                            if (isPressed) {
                                isPressed = false
                                onPressChanged(false)
                            }
                        }
                    }
                }
            }
    ) {
        val size = Size(size.width, size.height)

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = if (isPressed) {
                    listOf(Color.LightGray, Color.Gray)
                } else {
                    listOf(Color(0xFF282830), Color(0xFF14141A))
                }
            ),
            size = size,
            cornerRadius = CornerRadius(16f, 16f)
        )

        drawRoundRect(
            color = Color.White.copy(alpha = 0.08f),
            size = size,
            cornerRadius = CornerRadius(16f, 16f),
            style = Stroke(width = 1f)
        )

        val textLayout = textMeasurer.measure(
            text = AnnotatedString(label),
            style = TextStyle(
                color = if (isPressed) Color.Black else TextSecondary,
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold
            )
        )
        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(
                (size.width - textLayout.size.width) / 2f,
                (size.height - textLayout.size.height) / 2f
            )
        )
    }
}

/**
 * Virtual Trigger with live depth visualization (0.0 - 1.0)
 * Simulates L2/R2 analog triggers with curved trigger shape and fill bar
 */
@Composable
fun VirtualTrigger(
    modifier: Modifier = Modifier,
    label: String = "",
    color: Color = AccentBlue,
    triggerValue: Float, // 0.0 to 1.0
    onValueChange: (Float) -> Unit = {},
    isLeft: Boolean = true
) {
    val triggerHeight = 48.dp
    val triggerWidth = 75.dp
    val trackWidth = 60.dp
    val trackCornerRadius = 4.dp

    // Local state for drag gesture - use mutableState to avoid recomposition issues
    val currentTriggerValue = remember { mutableStateOf(triggerValue) }
    val dragStartY = remember { mutableStateOf(0f) }
    val dragStartValue = remember { mutableStateOf(0f) }
    val isDragging = remember { mutableStateOf(false) }
    val textMeasurer = rememberTextMeasurer()
    val pctMeasurer = rememberTextMeasurer()
    val haptic = LocalHapticFeedback.current

    // Sync currentTriggerValue from parent when not dragging
    LaunchedEffect(triggerValue) {
        if (!isDragging.value) {
            currentTriggerValue.value = triggerValue
        }
    }

    // Pre-compute paths that don't depend on triggerValue (only on size and isLeft)
    // These will be recreated when size or isLeft changes
    val triggerPath = remember(triggerWidth, triggerHeight, isLeft) {
        mutableStateOf<Path?>(null)
    }
    val highlightPath = remember(triggerWidth, triggerHeight, isLeft) {
        mutableStateOf<Path?>(null)
    }
    val shadowPath = remember(triggerWidth, triggerHeight, isLeft) {
        mutableStateOf<Path?>(null)
    }

    // Update paths when Canvas size is known
    val canvasSize = remember { mutableStateOf<Size?>(null) }

    Canvas(
        modifier = modifier
            .size(triggerWidth, triggerHeight)
            .pointerInput(Unit) { // Use Unit as key to avoid re-registration
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.pressed) {
                            change.consume()
                            if (!isDragging.value) {
                                isDragging.value = true
                                dragStartY.value = change.position.y
                                dragStartValue.value = currentTriggerValue.value
                                // Haptic feedback on press start
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            val dy = dragStartY.value - change.position.y // Up = increase
                            val newValue = (dragStartValue.value + dy / triggerHeight.toPx()).coerceIn(0f, 1f)
                            currentTriggerValue.value = newValue
                            onValueChange(newValue)
                        } else {
                            if (isDragging.value) {
                                isDragging.value = false
                                // Haptic feedback on release
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
                }
            }
    ) {
        val size = Size(size.width, size.height)
        val cx = size.width / 2f

        // Update cached paths on first draw or size change
        if (canvasSize.value != size) {
            canvasSize.value = size
            // Trigger path
            triggerPath.value = Path().apply {
                val curveStartY = size.height * 0.85f
                val curvePeakY = size.height * 0.15f
                val curveWidth = 24.dp.toPx()

                moveTo(cx, size.height * 0.95f)
                if (isLeft) {
                    cubicTo(
                        cx - curveWidth * 0.5f, size.height * 0.9f,
                        cx - curveWidth, curveStartY,
                        cx - curveWidth * 0.3f, curveStartY
                    )
                    lineTo(cx - curveWidth * 0.3f, curvePeakY)
                    cubicTo(
                        cx - curveWidth * 0.5f, curvePeakY - 4.dp.toPx(),
                        cx + curveWidth * 0.5f, curvePeakY - 4.dp.toPx(),
                        cx + curveWidth * 0.3f, curvePeakY
                    )
                    lineTo(cx + curveWidth * 0.3f, curveStartY)
                    cubicTo(
                        cx + curveWidth, curveStartY,
                        cx + curveWidth * 0.5f, size.height * 0.9f,
                        cx, size.height * 0.95f
                    )
                } else {
                    cubicTo(
                        cx + curveWidth * 0.5f, size.height * 0.9f,
                        cx + curveWidth, curveStartY,
                        cx + curveWidth * 0.3f, curveStartY
                    )
                    lineTo(cx + curveWidth * 0.3f, curvePeakY)
                    cubicTo(
                        cx + curveWidth * 0.5f, curvePeakY - 4.dp.toPx(),
                        cx - curveWidth * 0.5f, curvePeakY - 4.dp.toPx(),
                        cx - curveWidth * 0.3f, curvePeakY
                    )
                    lineTo(cx - curveWidth * 0.3f, curveStartY)
                    cubicTo(
                        cx - curveWidth, curveStartY,
                        cx - curveWidth * 0.5f, size.height * 0.9f,
                        cx, size.height * 0.95f
                    )
                }
                close()
            }

            // Shadow path
            shadowPath.value = Path().apply {
                addPath(triggerPath.value!!)
                translate(Offset(1f, 2f))
            }

            // Highlight path
            highlightPath.value = Path().apply {
                val curveStartY = size.height * 0.85f
                val curvePeakY = size.height * 0.18f
                val curveWidth = 24.dp.toPx()

                moveTo(cx - curveWidth * 0.2f, size.height * 0.9f)
                if (isLeft) {
                    cubicTo(
                        cx - curveWidth * 0.4f, size.height * 0.88f,
                        cx - curveWidth * 0.6f, curveStartY,
                        cx - curveWidth * 0.2f, curveStartY
                    )
                    lineTo(cx - curveWidth * 0.2f, curvePeakY)
                    cubicTo(
                        cx - curveWidth * 0.3f, curvePeakY - 3.dp.toPx(),
                        cx, curvePeakY - 3.dp.toPx(),
                        cx * 0.1f + curveWidth * 0.1f, curvePeakY
                    )
                } else {
                    cubicTo(
                        cx + curveWidth * 0.4f, size.height * 0.88f,
                        cx + curveWidth * 0.6f, curveStartY,
                        cx + curveWidth * 0.2f, curveStartY
                    )
                    lineTo(cx + curveWidth * 0.2f, curvePeakY)
                    cubicTo(
                        cx + curveWidth * 0.3f, curvePeakY - 3.dp.toPx(),
                        cx, curvePeakY - 3.dp.toPx(),
                        cx - curveWidth * 0.1f, curvePeakY
                    )
                }
                close()
            }
        }

        // Trigger track (background groove)
        val trackRect = Rect(
            left = cx - trackWidth.toPx() / 2f,
            top = size.height * 0.15f,
            right = cx + trackWidth.toPx() / 2f,
            bottom = size.height * 0.85f
        )

        // Track background
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.6f),
            topLeft = trackRect.topLeft + Offset(1f, 1f),
            size = trackRect.size,
            cornerRadius = CornerRadius(trackCornerRadius.toPx())
        )
        drawRoundRect(
            color = Color(0xFF1A1A20),
            topLeft = trackRect.topLeft,
            size = trackRect.size,
            cornerRadius = CornerRadius(trackCornerRadius.toPx())
        )
        drawRoundRect(
            color = color.copy(alpha = 0.15f),
            topLeft = trackRect.topLeft,
            size = trackRect.size,
            cornerRadius = CornerRadius(trackCornerRadius.toPx()),
            style = Stroke(width = 1.5f)
        )

        // Live fill bar - grows from bottom to top based on triggerValue
        val trackHeight = trackRect.height
        val fillHeight = trackHeight * currentTriggerValue.value
        if (fillHeight > 2.dp.toPx()) {
            val fillRect = Rect(
                left = trackRect.left + 2.dp.toPx(),
                top = trackRect.bottom - fillHeight,
                right = trackRect.right - 2.dp.toPx(),
                bottom = trackRect.bottom - 2.dp.toPx()
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.9f),
                        color.copy(alpha = 0.6f),
                        color.copy(alpha = 0.3f)
                    )
                ),
                topLeft = fillRect.topLeft,
                size = fillRect.size,
                cornerRadius = CornerRadius(trackCornerRadius.toPx())
            )
            // Specular highlight on fill
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                ),
                topLeft = fillRect.topLeft,
                size = Size(fillRect.width, fillRect.height * 0.3f),
                cornerRadius = CornerRadius(trackCornerRadius.toPx())
            )
        }

        // Trigger body shadow
        shadowPath.value?.let {
            drawPath(
                path = it,
                color = Color.Black.copy(alpha = 0.5f)
            )
        }

        // Trigger body - solid color that changes with triggerValue
        val bodyColor = if (currentTriggerValue.value > 0.1f) {
            color.copy(alpha = 0.85f)
        } else {
            Color(0xFF2C2C35).copy(alpha = 0.8f)
        }
        triggerPath.value?.let {
            drawPath(
                path = it,
                color = bodyColor
            )
        }

        // Trigger border/bezel
        triggerPath.value?.let {
            drawPath(
                path = it,
                color = if (currentTriggerValue.value > 0.1f) color.copy(alpha = 0.6f) else color.copy(alpha = 0.2f),
                style = Stroke(width = 1.5f)
            )
        }

        // Specular highlight on trigger
        highlightPath.value?.let {
            drawPath(
                path = it,
                color = Color.White.copy(alpha = 0.12f)
            )
        }

        // Label text (re-measure if triggerValue crossed threshold)
        if (label.isNotEmpty()) {
            val layout = if ((triggerPath.value == null || (triggerValue > 0.1f) != (currentTriggerValue.value > 0.1f)) && !isDragging.value) {
                textMeasurer.measure(
                    text = AnnotatedString(label),
                    style = TextStyle(
                        color = if (currentTriggerValue.value > 0.1f) Color.White else color.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            } else {
                textMeasurer.measure(
                    text = AnnotatedString(label),
                    style = TextStyle(
                        color = if (currentTriggerValue.value > 0.1f) Color.White else color.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    (size.width - layout.size.width) / 2f,
                    size.height * 0.05f
                )
            )
        }

        // Percentage indicator at bottom
        val pctText = "${(currentTriggerValue.value * 100).toInt()}%"
        val pctLayout = pctMeasurer.measure(
            text = AnnotatedString(pctText),
            style = TextStyle(
                color = color.copy(alpha = 0.6f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        )
        drawText(
            textLayoutResult = pctLayout,
            topLeft = Offset(
                (size.width - pctLayout.size.width) / 2f,
                size.height - pctLayout.size.height - 2.dp.toPx()
            )
        )
    }
}