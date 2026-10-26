package com.unscroll.app

import android.app.Activity
import android.os.Bundle
import com.unscroll.app.data.db.BreakLog
import com.unscroll.app.service.BreakReminders
import com.unscroll.app.service.LimitActions

/**
 * "Take a break" on a break reminder. It has no screen: it records the choice
 * and shows the phone's home screen. (Android doesn't let a notification button
 * open another app's screen from a background receiver, so a tiny invisible
 * activity does it.)
 */
class BreakActionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getStringExtra(BreakReminders.EXTRA_PACKAGE)?.let { packageName ->
            BreakReminders.answer(this, packageName, intent.getIntExtra(BreakReminders.EXTRA_MINUTES, 0), BreakLog.BREAK)
        }
        startActivity(LimitActions.homeIntent())
        finish()
    }
}
