package com.habitdiary.app.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.habitdiary.app.style.CardStyle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun cardStyle_defaultsToIndexTab_andPersists() = runTest {
        val ds = PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "s.preferences_pb") }
        var changed = 0
        val settings = DataStoreSettings(ds) { changed++ }
        assertEquals(CardStyle.INDEX_TAB, settings.cardStyle.first())
        settings.setCardStyle(CardStyle.MASKING_TAPE)
        assertEquals(CardStyle.MASKING_TAPE, settings.cardStyle.first())
        assertEquals(1, changed)
    }

    @Test
    fun widgetBindings_bindLookupUnbind() = runTest {
        val ds = PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "w.preferences_pb") }
        val bindings = WidgetBindings(ds)
        assertNull(bindings.habitIdFor(7))
        bindings.bind(7, 42L)
        assertEquals(42L, bindings.habitIdFor(7))
        bindings.unbind(7)
        assertNull(bindings.habitIdFor(7))
    }
}
