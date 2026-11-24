package com.yangyws.dualscreenbrowser.browser

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.hypot

enum class GamepadTargetScreen {
    TOP,
    BOTTOM
}

interface GamepadActions {
    fun scrollActive(dy: Int)
    fun goBackActive()
    fun goForwardActive()
    fun previousTab()
    fun nextTab()
    fun refreshActive()
    fun swapScreens()
    fun toggleTargetScreen()
    fun toggleSpanMode()
    fun setSpanSource(source: SpanSource)
    fun moveCursor(dx: Float, dy: Float) {}
    fun clickAtCursor(): Boolean = false
}

object GamepadController {
    private const val SCROLL_STEP_PX = 180
    private const val STICK_DEADZONE = 0.22f
    private const val STICK_SCROLL_MULTIPLIER = 42f
    private const val CURSOR_SPEED_PX = 28f

    fun handleKeyEvent(
        event: KeyEvent,
        enabled: Boolean,
        isEditingText: Boolean,
        actions: GamepadActions
    ): Boolean {
        if (!enabled) return false

        if (event.action != KeyEvent.ACTION_DOWN) return false

        return when (event.keyCode) {
            KeyEvent.KEYCODE_BUTTON_START -> {
                if (event.repeatCount == 0) {
                    actions.toggleSpanMode()
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (isEditingText) {
                    false
                } else {
                    actions.scrollActive(-SCROLL_STEP_PX)
                    true
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (isEditingText) {
                    false
                } else {
                    actions.scrollActive(SCROLL_STEP_PX)
                    true
                }
            }
            KeyEvent.KEYCODE_BUTTON_L1 -> {
                actions.previousTab()
                true
            }
            KeyEvent.KEYCODE_BUTTON_R1 -> {
                actions.nextTab()
                true
            }
            KeyEvent.KEYCODE_BUTTON_L2 -> {
                actions.goBackActive()
                true
            }
            KeyEvent.KEYCODE_BUTTON_R2 -> {
                actions.goForwardActive()
                true
            }
            KeyEvent.KEYCODE_BUTTON_X -> {
                actions.swapScreens()
                true
            }
            KeyEvent.KEYCODE_BUTTON_Y -> {
                actions.refreshActive()
                true
            }
            KeyEvent.KEYCODE_BUTTON_SELECT -> {
                actions.toggleTargetScreen()
                true
            }
            KeyEvent.KEYCODE_BUTTON_A -> {
                if (!isEditingText) actions.clickAtCursor() else false
            }
            else -> false
        }
    }

    fun handleGenericMotionEvent(
        event: MotionEvent,
        enabled: Boolean,
        actions: GamepadActions
    ): Boolean {
        if (!enabled) return false
        val isJoystick = (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
            (event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
        if (!isJoystick || event.action != MotionEvent.ACTION_MOVE) return false

        val leftX = event.getAxisValue(MotionEvent.AXIS_X)
        val leftY = event.getAxisValue(MotionEvent.AXIS_Y)
        var handled = false
        if (hypot(leftX, leftY) > STICK_DEADZONE) {
            actions.moveCursor(leftX * CURSOR_SPEED_PX, leftY * CURSOR_SPEED_PX)
            handled = true
        }

        val rightY = event.getAxisValue(MotionEvent.AXIS_RZ)
        if (abs(rightY) > STICK_DEADZONE) {
            val dy = (rightY * STICK_SCROLL_MULTIPLIER).toInt()
            if (dy != 0) {
                actions.scrollActive(dy)
                handled = true
            }
        }
        return handled
    }
}
