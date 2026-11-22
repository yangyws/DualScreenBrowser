package com.yangyws.dualscreenbrowser.browser

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.abs

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
}

object GamepadController {
    private const val SCROLL_STEP_PX = 180
    private const val STICK_DEADZONE = 0.25f
    private const val STICK_SCROLL_MULTIPLIER = 42f

    fun handleKeyEvent(
        event: KeyEvent,
        enabled: Boolean,
        isEditingText: Boolean,
        actions: GamepadActions
    ): Boolean {
        if (!enabled || event.action != KeyEvent.ACTION_DOWN) return false

        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (isEditingText) false else {
                    actions.scrollActive(-SCROLL_STEP_PX)
                    true
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (isEditingText) false else {
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
            KeyEvent.KEYCODE_BUTTON_START -> {
                actions.toggleSpanMode()
                true
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

        val leftY = event.getAxisValue(MotionEvent.AXIS_Y)
        val rightY = event.getAxisValue(MotionEvent.AXIS_RZ)
        val dominantY = if (abs(rightY) > abs(leftY)) rightY else leftY
        if (abs(dominantY) <= STICK_DEADZONE) return false

        val dy = (dominantY * STICK_SCROLL_MULTIPLIER).toInt()
        if (dy != 0) {
            actions.scrollActive(dy)
            return true
        }
        return false
    }
}
