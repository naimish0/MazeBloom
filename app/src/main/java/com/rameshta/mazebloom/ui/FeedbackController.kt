package com.rameshta.mazebloom.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import com.rameshta.mazebloom.core.GameEvent
import com.rameshta.mazebloom.core.TransitionResult
import com.rameshta.mazebloom.data.PlayerSettings

class FeedbackController(private val view: View) : AutoCloseable {
    private val tones = ToneGenerator(AudioManager.STREAM_MUSIC, 28)

    fun present(result: TransitionResult, settings: PlayerSettings) {
        if (!result.isValid) {
            if (settings.sound) tones.startTone(ToneGenerator.TONE_PROP_NACK, 45)
            if (settings.haptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            return
        }
        val collected = result.orderedEvents.count { it is GameEvent.BudCollected }
        if (settings.sound) {
            tones.startTone(
                if (result.afterState.remainingBuds.isEmpty()) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_BEEP,
                if (collected > 0) 95 else 45,
            )
        }
        if (settings.haptics && collected > 0) {
            view.performHapticFeedback(
                if (result.afterState.remainingBuds.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.CONFIRM
                } else if (result.afterState.remainingBuds.isEmpty()) {
                    HapticFeedbackConstants.LONG_PRESS
                } else {
                    HapticFeedbackConstants.KEYBOARD_TAP
                },
            )
        }
    }

    override fun close() { tones.release() }
}
