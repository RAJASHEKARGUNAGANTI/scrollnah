package com.unscroll.app.data

/** Switches ScrollNah may need turned on, in the order the setup guide asks for them. */
enum class SetupStep { NOTIFICATIONS, OVERLAY, POPUPS, BATTERY }

/**
 * Which switches are on right now. null = this phone doesn't need that switch:
 * [popups] only exists on Xiaomi and vivo phones, and Google Pixels don't stop apps in the
 * background, so [battery] is null there.
 */
data class SetupFacts(
    val notifications: Boolean,
    val overlay: Boolean,
    val popups: Boolean?,
    val battery: Boolean?
)

/** Which features the user's choices rely on. */
data class SetupUses(
    val pauseScreen: Boolean, // Firm, Beast or focus mode
    val bubbles: Boolean      // "X min left" bubble or break-reminder bubble
)

/** Decides what the setup guide asks for, so nobody is asked for a switch they don't need. */
object SetupPlan {

    fun uses(strictness: Strictness, focusOn: Boolean, timeLeftBubble: Boolean, breaks: BreakSettings) = SetupUses(
        // Chill only sends notifications, but focus mode always uses the Pause screen.
        pauseScreen = strictness != Strictness.CHILL || focusOn,
        bubbles = timeLeftBubble || (breaks.enabled && breaks.useBubble)
    )

    /** The steps still to do on this phone for these choices, in order. Empty = all set. */
    fun missing(facts: SetupFacts, uses: SetupUses): List<SetupStep> = buildList {
        if (!facts.notifications) add(SetupStep.NOTIFICATIONS)
        if ((uses.pauseScreen || uses.bubbles) && !facts.overlay) add(SetupStep.OVERLAY)
        // The pop-up switch only matters for the Pause screen (bubbles are overlays, not screens).
        if (uses.pauseScreen && facts.popups == false) add(SetupStep.POPUPS)
        if (facts.battery == false) add(SetupStep.BATTERY)
    }

    /**
     * "Step [first] of [second]". [firstMissing] = what was missing when the guide first showed,
     * so the count stays the same while the user works through it (and grows if a new need appears).
     */
    fun position(firstMissing: Set<SetupStep>, missing: List<SetupStep>): Pair<Int, Int> {
        val total = (firstMissing + missing).size
        return (total - missing.size + 1) to total
    }
}
