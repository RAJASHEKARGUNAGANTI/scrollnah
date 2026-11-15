package com.unscroll.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class IdeaKindTest {

    @Test
    fun theStarterIdeasEachGetTheirOwnPicture() {
        assertEquals(IdeaKind.WATER, IdeaKind.of("Drink a glass of water"))
        assertEquals(IdeaKind.MOVE, IdeaKind.of("Stretch for one minute"))
        assertEquals(IdeaKind.OUTSIDE, IdeaKind.of("Step outside for some air"))
        assertEquals(IdeaKind.BOOK, IdeaKind.of("Read a few pages of a book"))
        assertEquals(IdeaKind.CALL, IdeaKind.of("Call someone you like"))
    }

    @Test
    fun ownIdeasArePickedFromTheirWords() {
        assertEquals(IdeaKind.CUP, IdeaKind.of("Make a cup of chai"))
        assertEquals(IdeaKind.MUSIC, IdeaKind.of("Listen to one song"))
        assertEquals(IdeaKind.WRITE, IdeaKind.of("Write 3 lines in my journal"))
        assertEquals(IdeaKind.REST, IdeaKind.of("Take a short nap"))
        assertEquals(IdeaKind.MOVE, IdeaKind.of("Do 10 push-ups"))
        assertEquals(IdeaKind.AIR, IdeaKind.of("Take 5 slow breaths"))
        assertEquals(IdeaKind.OUTSIDE, IdeaKind.of("Go for a walk"))
        assertEquals(IdeaKind.CALL, IdeaKind.of("Text a friend"))
    }

    @Test
    fun anythingElseGetsAHeart() {
        assertEquals(IdeaKind.OTHER, IdeaKind.of("Play with the dog"))
        assertEquals(IdeaKind.OTHER, IdeaKind.of(""))
    }
}
