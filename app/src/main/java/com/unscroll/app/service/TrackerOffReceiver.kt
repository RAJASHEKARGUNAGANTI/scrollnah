package com.unscroll.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat

/**
 * The "Turn off tracker" button on the payment-app notification: switches off the
 * Reels & Shorts tracker so UPI/bank apps like BHIM open, then says how to get it back.
 */
class TrackerOffReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationManagerCompat.from(context).cancel(LimitActions.PAYMENT_HELP_NOTIFICATION_ID)
        val message = if (FeatureWatchService.turnOff()) {
            "Tracker off. You can pay now. Turn it back on in ScrollNah > Limits."
        } else {
            "The tracker is already off."
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
