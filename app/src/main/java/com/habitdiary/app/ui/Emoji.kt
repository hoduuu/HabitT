package com.habitdiary.app.ui

import android.icu.text.BreakIterator

/** 마지막 글자(그래핌) 하나만 남긴다. 이모지 칸에 새 이모지를 치면 이전 것을 대체하게 하기 위함. */
fun lastGrapheme(text: String): String {
    if (text.isEmpty()) return ""
    val it = BreakIterator.getCharacterInstance()
    it.setText(text)
    val end = it.last()
    val start = it.previous()
    return if (start == BreakIterator.DONE) text else text.substring(start, end)
}
