package com.yangyws.dualscreenbrowser.browser

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.webkit.WebView
import kotlin.math.abs
import kotlin.math.hypot

enum class GamepadTargetScreen {
    TOP,
    BOTTOM
}

interface GamepadActions {
    fun scrollActive(dx: Int, dy: Int)
    fun goBackActive()
    fun goForwardActive()
    fun previousTab()
    fun nextTab()
    fun refreshActive()
    fun swapScreens()
    fun toggleTargetScreen()
    fun setTargetScreen(target: GamepadTargetScreen) {}
    fun toggleSpanMode()
    fun setSpanSource(source: SpanSource)
    fun moveCursor(dx: Float, dy: Float) {}
    fun clickAtCursor(): Boolean = false
    fun moveWebFocus(keyCode: Int): Boolean = false
    fun clickWebFocus(): Boolean = false
    fun openMenu() {}
    fun dispatchToBottomPopup(event: KeyEvent): Boolean = false
}

object GamepadController {
    private const val SCROLL_STEP_PX = 180
    private const val STICK_DEADZONE = 0.22f
    private const val STICK_SCROLL_MAX_PX = 22f
    private const val CURSOR_SPEED_PX = 28f
    private const val DPAD_REPEAT_INTERVAL_MS = 145L

    private var selectPressed = false
    private var selectComboTriggered = false
    private var startPressed = false
    private var startComboTriggered = false
    private var scrollRemainderX = 0f
    private var scrollRemainderY = 0f
    private var lastDpadMoveMs = 0L

    val isSelectPressed: Boolean
        get() = selectPressed

    val isStartPressed: Boolean
        get() = startPressed

    val isStartComboTriggered: Boolean
        get() = startComboTriggered

    fun handleKeyEvent(
        event: KeyEvent,
        enabled: Boolean,
        isEditingText: Boolean,
        actions: GamepadActions
    ): Boolean {
        if (!enabled) {
            selectPressed = false
            selectComboTriggered = false
            startPressed = false
            startComboTriggered = false
            return false
        }

        if (event.action == KeyEvent.ACTION_UP) {
            val canceled = (event.flags and KeyEvent.FLAG_CANCELED) != 0
            return when (event.keyCode) {
                KeyEvent.KEYCODE_BUTTON_SELECT -> {
                    val shouldToggle = selectPressed && !selectComboTriggered && !canceled
                    selectPressed = false
                    selectComboTriggered = false
                    if (shouldToggle) {
                        actions.toggleSpanMode()
                    }
                    true
                }
                KeyEvent.KEYCODE_BUTTON_START -> {
                    val shouldOpenMenu = startPressed && !startComboTriggered && !canceled
                    startPressed = false
                    startComboTriggered = false
                    if (shouldOpenMenu) {
                        actions.openMenu()
                    }
                    true
                }
                else -> false
            }
        }

        if (event.action != KeyEvent.ACTION_DOWN) return false

        return when (event.keyCode) {
            KeyEvent.KEYCODE_BUTTON_START -> {
                if (event.repeatCount == 0) {
                    startPressed = true
                    startComboTriggered = false
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                when {
                    selectPressed -> {
                        if (event.repeatCount == 0) {
                            selectComboTriggered = true
                            actions.setSpanSource(SpanSource.TOP)
                        }
                        true
                    }
                    startPressed -> {
                        if (event.repeatCount == 0) {
                            startComboTriggered = true
                            actions.setTargetScreen(GamepadTargetScreen.TOP)
                        }
                        true
                    }
                    isEditingText -> false
                    else -> dispatchDpadFocusMove(event, actions)
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                when {
                    selectPressed -> {
                        if (event.repeatCount == 0) {
                            selectComboTriggered = true
                            actions.setSpanSource(SpanSource.BOTTOM)
                        }
                        true
                    }
                    startPressed -> {
                        if (event.repeatCount == 0) {
                            startComboTriggered = true
                            actions.setTargetScreen(GamepadTargetScreen.BOTTOM)
                        }
                        true
                    }
                    isEditingText -> false
                    else -> dispatchDpadFocusMove(event, actions)
                }
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (isEditingText) false else dispatchDpadFocusMove(event, actions)
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
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (isEditingText) false else dispatchDpadFocusMove(event, actions)
            }
            KeyEvent.KEYCODE_BUTTON_SELECT -> {
                if (event.repeatCount == 0) {
                    selectPressed = true
                    selectComboTriggered = false
                }
                true
            }
            KeyEvent.KEYCODE_BUTTON_A -> {
                if (isEditingText) false else (actions.clickAtCursor() || actions.clickWebFocus())
            }
            KeyEvent.KEYCODE_BUTTON_B -> {
                if (isEditingText) {
                    false
                } else {
                    if (event.repeatCount == 0) {
                        actions.goBackActive()
                    }
                    true
                }
            }
            else -> false
        }
    }

    private fun dispatchDpadFocusMove(event: KeyEvent, actions: GamepadActions): Boolean {
        val now = android.os.SystemClock.uptimeMillis()
        if (event.repeatCount > 0 && now - lastDpadMoveMs < DPAD_REPEAT_INTERVAL_MS) {
            return true
        }
        lastDpadMoveMs = now
        return actions.moveWebFocus(event.keyCode)
    }

    fun moveWebFocus(webView: WebView?, keyCode: Int): Boolean {
        val wv = webView ?: return false
        val dir = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> "up"
            KeyEvent.KEYCODE_DPAD_DOWN -> "down"
            KeyEvent.KEYCODE_DPAD_LEFT -> "left"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "right"
            else -> return false
        }
        val js = """
            (function() {
              var dir = '$dir';
              var styleId = '__dsb_focus_style';
              if (!document.getElementById(styleId)) {
                var st = document.createElement('style');
                st.id = styleId;
                st.textContent = '.__dsb_focus_ring { outline: 2.5px solid #3B82F6 !important; outline-offset: 2px !important; border-radius: 4px !important; box-shadow: 0 0 0 3px rgba(59,130,246,0.28) !important; }';
                (document.head || document.documentElement).appendChild(st);
              }
              var sel = 'a[href],button,input:not([type="hidden"]),select,textarea,summary,[role="button"],[role="link"],[role="tab"],[tabindex]:not([tabindex="-1"])';
              var all = Array.prototype.slice.call(document.querySelectorAll(sel)).filter(function(el) {
                var cs = getComputedStyle(el);
                if (cs.display === 'none' || cs.visibility === 'hidden' || parseFloat(cs.opacity) === 0) return false;
                var r = el.getBoundingClientRect();
                return r.width > 1 && r.height > 1;
              });
              if (!all.length) return;
              var cur = window.__dsbFocusedEl;
              if (!cur || !document.contains(cur)) {
                var ae = document.activeElement;
                cur = (ae && ae !== document.body && ae !== document.documentElement) ? ae : null;
              }
              function inView(r) {
                return r.bottom >= 0 && r.top <= window.innerHeight && r.right >= 0 && r.left <= window.innerWidth;
              }
              if (cur && !inView(cur.getBoundingClientRect())) {
                if (cur.classList) cur.classList.remove('__dsb_focus_ring');
                cur = null;
              }
              var next = null;
              if (!cur) {
                var visible = all.filter(function(el) { return inView(el.getBoundingClientRect()); });
                var pool = visible.length ? visible : all;
                pool.sort(function(a, b) {
                  var ra = a.getBoundingClientRect(), rb = b.getBoundingClientRect();
                  return (ra.top - rb.top) || (ra.left - rb.left);
                });
                next = pool[0];
              } else {
                var cr = cur.getBoundingClientRect();
                var cx = cr.left + cr.width / 2;
                var cy = cr.top + cr.height / 2;
                var bestScore = Infinity;
                for (var i = 0; i < all.length; i++) {
                  var el = all[i];
                  if (el === cur) continue;
                  var r = el.getBoundingClientRect();
                  var ex = r.left + r.width / 2;
                  var ey = r.top + r.height / 2;
                  var dx = ex - cx;
                  var dy = ey - cy;
                  var ok = false;
                  var primary = 0, secondary = 0;
                  if (dir === 'down' && dy > 4) { ok = true; primary = dy; secondary = Math.abs(dx); }
                  else if (dir === 'up' && dy < -4) { ok = true; primary = -dy; secondary = Math.abs(dx); }
                  else if (dir === 'right' && dx > 4) { ok = true; primary = dx; secondary = Math.abs(dy); }
                  else if (dir === 'left' && dx < -4) { ok = true; primary = -dx; secondary = Math.abs(dy); }
                  if (ok) {
                    var score = primary + secondary * 2.5;
                    if (score < bestScore) { bestScore = score; next = el; }
                  }
                }
              }
              if (next) {
                if (cur && cur.classList) cur.classList.remove('__dsb_focus_ring');
                if (next.classList) next.classList.add('__dsb_focus_ring');
                window.__dsbFocusedEl = next;
                try { next.focus({ preventScroll: true }); } catch (e) {}
                next.scrollIntoView({ behavior: 'auto', block: 'nearest', inline: 'nearest' });
              } else {
                var sx = dir === 'left' ? -120 : (dir === 'right' ? 120 : 0);
                var sy = dir === 'up' ? -140 : (dir === 'down' ? 140 : 0);
                window.scrollBy(sx, sy);
              }
            })();
        """.trimIndent()
        wv.post { wv.evaluateJavascript(js, null) }
        return true
    }

    fun clickWebFocus(webView: WebView?): Boolean {
        val wv = webView ?: return false
        val js = """
            (function() {
              var el = window.__dsbFocusedEl;
              if (!el || !document.contains(el)) {
                var ae = document.activeElement;
                if (ae && ae !== document.body && ae !== document.documentElement) el = ae;
              }
              if (el) {
                try { el.focus(); } catch (e) {}
                try { el.click(); } catch (e) {}
                return true;
              }
              return false;
            })();
        """.trimIndent()
        wv.post { wv.evaluateJavascript(js, null) }
        return true
    }

    fun clearWebFocus(webView: WebView?) {
        val wv = webView ?: return
        val js = """
            (function() {
              var cur = window.__dsbFocusedEl;
              if (cur && cur.classList) cur.classList.remove('__dsb_focus_ring');
              window.__dsbFocusedEl = null;
            })();
        """.trimIndent()
        wv.post { wv.evaluateJavascript(js, null) }
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

        val rightX = event.getAxisValue(MotionEvent.AXIS_Z)
        val rightY = event.getAxisValue(MotionEvent.AXIS_RZ)
        if (abs(rightX) > STICK_DEADZONE || abs(rightY) > STICK_DEADZONE) {
            fun smoothDelta(axis: Float): Float {
                val mag = abs(axis)
                if (mag <= STICK_DEADZONE) return 0f
                val norm = ((mag - STICK_DEADZONE) / (1f - STICK_DEADZONE)).coerceIn(0f, 1f)
                val curved = java.lang.Math.pow(norm.toDouble(), 1.8).toFloat()
                return kotlin.math.sign(axis) * curved * STICK_SCROLL_MAX_PX
            }
            if (abs(rightX) > STICK_DEADZONE) scrollRemainderX += smoothDelta(rightX) else scrollRemainderX = 0f
            if (abs(rightY) > STICK_DEADZONE) scrollRemainderY += smoothDelta(rightY) else scrollRemainderY = 0f
            val dx = scrollRemainderX.toInt()
            val dy = scrollRemainderY.toInt()
            scrollRemainderX -= dx
            scrollRemainderY -= dy
            if (dx != 0 || dy != 0) {
                actions.scrollActive(dx, dy)
            }
            handled = true
        } else {
            scrollRemainderX = 0f
            scrollRemainderY = 0f
        }
        return handled
    }
}
