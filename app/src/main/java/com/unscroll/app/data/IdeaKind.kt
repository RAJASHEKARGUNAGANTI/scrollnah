package com.unscroll.app.data

/**
 * Which little picture goes with a "Do this instead" idea. The ideas are the user's own words,
 * so it's picked from the words in them ("Drink a glass of water" -> water). Anything else gets
 * a heart: something you like. Pure logic (tested); the icons are drawn in ui/components/Icons.kt.
 */
enum class IdeaKind {
    WATER, CUP, MOVE, OUTSIDE, AIR, BOOK, CALL, MUSIC, WRITE, REST, OTHER;

    companion object {
        // Checked in this order; a word in the idea that starts with one of these picks it.
        private val WORDS = listOf(
            WATER to listOf("water", "hydrat"),
            CUP to listOf("tea", "coffee", "chai", "cup"),
            MOVE to listOf("stretch", "yoga", "push", "squat", "exercise", "workout", "jump", "dance", "gym", "plank"),
            OUTSIDE to listOf("outside", "outdoor", "walk", "garden", "park", "sun", "balcony", "run"),
            AIR to listOf("air", "breath", "breathe", "meditat"),
            BOOK to listOf("read", "book", "page", "study"),
            CALL to listOf("call", "phone", "text", "message", "friend", "mom", "mum", "dad", "family", "talk"),
            MUSIC to listOf("music", "song", "sing", "listen", "guitar", "piano", "playlist"),
            WRITE to listOf("write", "journal", "diary", "draw", "sketch", "note", "plan"),
            REST to listOf("sleep", "nap", "rest", "bed", "lie")
        )

        fun of(idea: String): IdeaKind {
            val words = idea.lowercase().split(Regex("[^a-z]+")).filter { it.isNotEmpty() }
            return WORDS.firstOrNull { (_, starts) -> words.any { w -> starts.any { w.startsWith(it) } } }?.first ?: OTHER
        }
    }
}
