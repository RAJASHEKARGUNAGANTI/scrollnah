package com.unscroll.app.ui.navigation

enum class Destination(val route: String, val label: String) {
    HOME("home", "Home"),
    LIMITS("limits", "Limits"),
    STATS("stats", "Stats"),
    PAUSE("pause", "Pause")
}

/** Not a bottom-nav tab, so it lives outside the enum. */
const val ADD_APP_ROUTE = "add_app"
const val SETTINGS_ROUTE = "settings"
const val BREAKS_ROUTE = "breaks"
const val GARDEN_ROUTE = "garden"
const val WEEK_REVIEW_ROUTE = "week_review"
