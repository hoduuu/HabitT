package com.habitdiary.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EmojiTest {
    @Test
    fun keepsOnlyLastTypedGrapheme() {
        assertEquals("", lastGrapheme(""))
        assertEquals("🌙", lastGrapheme("🌙"))
        assertEquals("📖", lastGrapheme("🌙📖"))
        assertEquals("🌙", lastGrapheme("a🌙"))
        assertEquals("👍🏽", lastGrapheme("🌙👍🏽"))          // 피부색 수식자 포함 한 글자
        assertEquals("👨‍👩‍👧", lastGrapheme("🌙👨‍👩‍👧"))     // ZWJ 가족 이모지 한 글자
    }
}
