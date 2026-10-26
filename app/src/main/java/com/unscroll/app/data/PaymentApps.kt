package com.unscroll.app.data

/**
 * UPI and banking apps. Many of them refuse to open while any non-system app has an
 * Accessibility service switched on (a fraud protection), and ask the user to close
 * or uninstall that app. ScrollNah's optional Reels & Shorts tracker is such a
 * service, so ScrollNah explains this and offers a quick way to turn the tracker off.
 * ScrollNah never draws its bubble over these apps either.
 */
object PaymentApps {
    private val packages = setOf(
        "in.org.npci.upiapp",                     // BHIM
        "com.phonepe.app",                        // PhonePe
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "net.one97.paytm",                        // Paytm
        "com.sbi.lotusintouch",                   // SBI YONO
        "com.csam.icici.bank.imobile",            // ICICI iMobile
        "com.snapwork.hdfc",                      // HDFC Bank
        "com.axis.mobile",                        // Axis Mobile
        "com.msf.kbank.mobile",                   // Kotak
        "com.dreamplug.androidapp",               // CRED
        "com.mobikwik_new"                        // MobiKwik
    )

    fun contains(packageName: String?) = packageName in packages
}
