package com.example.human_posture

import org.junit.Assert.*
import org.junit.Test

class BatchSelectionTest {
    private val images = (1..15).map(Int::toString)
    private fun choose(count: Int? = 5, manual: Set<String>? = null, onlyGt: Boolean = false,
        hasGt: (String) -> Boolean = { true }) = BatchSelection.choose(images, count, manual, onlyGt, { it }, hasGt)
    @Test fun automaticTakesExactlyNInSourceOrder() { assertEquals(images.take(5), choose()) }
    @Test fun manualUsesChosenImagesAndIgnoresAutomaticCount() {
        val keys = setOf("2", "4", "8", "11", "15")
        assertEquals(images.filter { it in keys }, choose(count = 20, manual = keys))
    }
    @Test fun automaticFiltersBeforeTakingN() {
        assertEquals(listOf("2","4","6","8","10"), choose(onlyGt = true, hasGt = { it.toInt() % 2 == 0 }))
    }
    @Test(expected = IllegalArgumentException::class) fun insufficientImagesAreNotSilentlyTruncated() { choose(count = 20) }
    @Test(expected = IllegalArgumentException::class) fun manualMissingGtDoesNotSilentlyChangeSelection() {
        choose(manual = images.take(5).toSet(), onlyGt = true, hasGt = { it != "2" })
    }
    @Test(expected = IllegalArgumentException::class) fun emptyManualSelectionDoesNotFallBackToAutomatic() { choose(manual = emptySet()) }
    @Test(expected = IllegalArgumentException::class) fun requiresMultipleOfFive() { choose(count = 3) }
}
