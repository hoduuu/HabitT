package com.habitdiary.app.render

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.habitdiary.app.R

data class CardFonts(val gaegu: Typeface, val gaeguBold: Typeface, val gamja: Typeface) {
    companion object {
        fun load(context: Context) = CardFonts(
            gaegu = ResourcesCompat.getFont(context, R.font.gaegu_regular) ?: Typeface.DEFAULT,
            gaeguBold = ResourcesCompat.getFont(context, R.font.gaegu_bold) ?: Typeface.DEFAULT_BOLD,
            gamja = ResourcesCompat.getFont(context, R.font.gamja_flower) ?: Typeface.DEFAULT,
        )
    }
}
