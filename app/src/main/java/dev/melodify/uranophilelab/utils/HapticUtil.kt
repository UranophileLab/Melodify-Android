package dev.melodify.uranophilelab.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Tactile micro-haptic utility for crisp, satisfying feedback across playback controls.
 */
object HapticUtil {

    /**
     * Crisp, light tactile click for standard button taps (play/pause, skip, shuffle, menu items)
     */
    fun tap(view: View?) {
        if (view == null) return
        try {
            val success = view.performHapticFeedback(
                HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            if (!success) {
                fallbackVibrate(view.context, 12)
            }
        } catch (_: Exception) {
            fallbackVibrate(view.context, 12)
        }
    }

    /**
     * Very subtle micro-tick for continuous scrubbing (seekbar movements, drag reorder moves)
     */
    fun tick(view: View?) {
        if (view == null) return
        try {
            val feedbackConstant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                HapticFeedbackConstants.CLOCK_TICK
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
            val success = view.performHapticFeedback(
                feedbackConstant,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            if (!success) {
                fallbackVibrate(view.context, 6)
            }
        } catch (_: Exception) {
            fallbackVibrate(view.context, 6)
        }
    }

    /**
     * Confirmation tactile pulse for actions like adding to favorites, adding to playlist, drag release
     */
    fun confirm(view: View?) {
        if (view == null) return
        try {
            val feedbackConstant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
            val success = view.performHapticFeedback(
                feedbackConstant,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            if (!success) {
                fallbackVibrate(view.context, 20)
            }
        } catch (_: Exception) {
            fallbackVibrate(view.context, 20)
        }
    }

    private fun fallbackVibrate(context: Context, durationMs: Long) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val amplitude = if (durationMs <= 10) 60 else 140
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {}
    }
}
